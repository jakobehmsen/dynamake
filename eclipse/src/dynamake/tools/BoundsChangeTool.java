package dynamake.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RelativeCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.ModelRootLocation;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.NewChangeTransactionHandler;
import dynamake.transcription.Trigger;

public abstract class BoundsChangeTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		if(viewPressedOn != null) {
			ModelComponent newTargetOver = targetPresenter.getTargetOver();

			final ModelComponent localViewPressedOn = viewPressedOn;
			final Rectangle currentBounds = SwingUtilities.convertRectangle(productionPanel, interactionPresenter.getSelectionFrameBounds(), (JComponent)source);
			collector.afterNextTrigger(new Runnable() {
				@Override
				public void run() {
					((JComponent)source).add((JComponent)localViewPressedOn);
					((JComponent)localViewPressedOn).setBounds(currentBounds);
				}
			});
			
			targetPresenter.reset(collector);
			
			final ModelComponent selection = interactionPresenter.getSelection();
			final Rectangle selectionBounds = interactionPresenter.getSelectionFrameBounds();
			final Rectangle effectBounds = interactionPresenter.getEffectFrameBounds();

			interactionPresenter.reset(collector);
			
			collector.flushNextTrigger();
			
			if(!selectionBounds.equals(effectBounds)) {
				if(relativePosition.isInCenter()) {
					if(newTargetOver.getModelTranscriber() != selection.getModelTranscriber().getParent()) {
						// Moving to other canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(
							productionPanel, effectBounds, (JComponent)newTargetOver);

						final ModelComponent targetOver = newTargetOver;

						// Reference is closest common ancestor
						collector.execute(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								ModelComponent referenceMC = ModelComponent.Util.closestCommonAncestor(source, targetOver);
								
								Location locationOfSource = ModelComponent.Util.locationFromAncestor(referenceMC, source);
								Location locationOfTarget = ModelComponent.Util.locationFromAncestor(referenceMC, targetOver);
								
								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
								
								CanvasModel.appendMoveTransaction(pendingCommands, productionPanel.livePanel, source, selection, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget);
								
								collector.startTransaction(referenceMC.getModelBehind(), NewChangeTransactionHandler.class);
								PendingCommandFactory.Util.executeSequence(collector, referenceMC.getModelBehind(), pendingCommands, NewChangeTransactionHandler.class);
								collector.commitTransaction();
							}
						});
					} else {
						// Moving within same canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);
						
						collector.execute(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								Location locationOfMovedTargetFromSource = ((CanvasModel)source.getModelBehind()).getLocationOf(selection.getModelBehind());
								
								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
								
								pendingCommands.add(new PendingCommandState<Model>(
									new RelativeCommand<Model>(locationOfMovedTargetFromSource, new SetPropertyCommand("X", new Fraction(droppedBounds.x))),
									new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
								));
								pendingCommands.add(new PendingCommandState<Model>(
									new RelativeCommand<Model>(locationOfMovedTargetFromSource, new SetPropertyCommand("Y", new Fraction(droppedBounds.y))),
									new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
								));
								
								PendingCommandFactory.Util.executeSequence(collector, source.getModelBehind(), pendingCommands, NewChangeTransactionHandler.class);
							}
						});
					}
				} else {
					// Changing bounds within the same canvas
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);
					
					collector.execute(new Trigger<Model>() {
						@Override
						public void run(Collector<Model> collector) {
							Location locationOfTargetFromSource = ((CanvasModel)source.getModelBehind()).getLocationOf(selection.getModelBehind());
							ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();

							appendCommandStatesForResize(pendingCommands, selection, newBounds);

							for(int i = 0; i < pendingCommands.size(); i++) {
								CommandState<Model> offsetCommand = pendingCommands.get(i).offset(locationOfTargetFromSource);
								pendingCommands.set(i, offsetCommand);
							}
							
							PendingCommandFactory.Util.executeSequence(collector, selection.getModelBehind(), pendingCommands, NewChangeTransactionHandler.class);
						}
					});
				}
				
				collector.commitTransaction();
			} else {
				collector.rejectTransaction();
			}
		}
	}

	protected abstract void appendCommandStatesForResize(List<CommandState<Model>> commandStates, ModelComponent selection, Rectangle newBounds);
	
	private Point mouseDown;
	private ModelComponent viewPressedOn;
	private ModelComponent source;
	private RelativePosition relativePosition;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			source = ModelComponent.Util.getParent(targetModelComponent);
			// Start transaction relative to the parent of the model, for which the bounds are being changed
			collector.startTransaction(source.getModelBehind(), NewChangeTransactionHandler.class);
			
			Point referencePoint = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)targetModelComponent);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
			
			relativePosition = new RelativePosition(referencePoint, ((JComponent)targetModelComponent).getSize());
			final Cursor cursor = relativePosition.getCursor();
			
			final InteractionPresenter locationInteractionPresenter = interactionPresenter;
			collector.afterNextTrigger(new Runnable() {
				@Override
				public void run() {
					locationInteractionPresenter.setSelectionFrameCursor(cursor);
					locationInteractionPresenter.setEffectFrameCursor(cursor);
				}
			});
			
			targetPresenter = new TargetPresenter(
				productionPanel,
				new TargetPresenter.Behavior() {
					@Override
					public Color getColorForTarget(ModelComponent target) {
						return ProductionPanel.TARGET_OVER_COLOR;
					}
					
					@Override
					public boolean acceptsTarget(ModelComponent target) {
						return true;
					}
				}
			);

			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, modelOver);
			targetPresenter.update(newTargetOver, collector);
			
			mouseDown = mousePoint;

			final ModelComponent localViewPressedOn = viewPressedOn;
			collector.afterNextTrigger(new Runnable() {
				@Override
				public void run() {
					productionPanel.add((JComponent)localViewPressedOn);
					((JComponent)localViewPressedOn).setBounds(interactionPresenter.getEffectFrameBounds());
				}
			});
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) {
		if(mouseDown != null && interactionPresenter.getSelection() != productionPanel.contentView.getBindingTarget()) {
			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, interactionPresenter.getSelection());
			targetPresenter.update(newTargetOver, collector);
			
			final Rectangle newEffectBounds = relativePosition.resize(
				interactionPresenter.getSelectionFrameLocation(), 
				interactionPresenter.getSelectionFrameSize(), 
				mouseDown, 
				interactionPresenter.getEffectFrameBounds(), 
				mousePoint);
			
			interactionPresenter.changeEffectFrameDirect(newEffectBounds, collector);
			
			final ModelComponent localViewPressedOn = viewPressedOn;
			collector.afterNextTrigger(new Runnable() {
				@Override
				public void run() {
					((JComponent)localViewPressedOn).setBounds(newEffectBounds);
				}
			});
		}
	}
	
	private ModelComponent getTargetOver(ProductionPanel productionPanel, ModelComponent modelOver, ModelComponent selection) {
		ModelComponent newTargetOver;
		
		if(relativePosition.isInCenter()) {
			// Moving
			newTargetOver = modelOver;
			
			if(((JComponent)selection).isAncestorOf((JComponent)newTargetOver))
				newTargetOver = selection;
			
			if(newTargetOver == selection)
				newTargetOver = ModelComponent.Util.closestModelComponent(((JComponent)newTargetOver).getParent());
		} else {
			// Resizing
			newTargetOver = source;
		}
		
		newTargetOver = ModelComponent.Util.closestCanvasModelComponent(newTargetOver);
		
		return newTargetOver;
	}
	
	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		if(mouseDown != null) {
			final ModelComponent localViewPressedOn = viewPressedOn;
			final Rectangle currentBounds = SwingUtilities.convertRectangle(productionPanel, interactionPresenter.getSelectionFrameBounds(), (JComponent)source);
			collector.afterNextTrigger(new Runnable() {
				@Override
				public void run() {
					((JComponent)source).add((JComponent)localViewPressedOn);
					((JComponent)localViewPressedOn).setBounds(currentBounds);
				}
			});
			
			targetPresenter.reset(collector);
			interactionPresenter.reset(collector);
			
			collector.flushNextTrigger();
		}
	}
}
