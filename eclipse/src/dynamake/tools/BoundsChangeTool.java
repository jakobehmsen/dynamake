package dynamake.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
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
								
								ArrayList<Object> pendingCommands = new ArrayList<Object>();
								
								CanvasModel.appendMoveTransaction2(pendingCommands, productionPanel.livePanel, source, selection, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget, collector);
								
								collector.startTransaction(referenceMC.getModelBehind(), NewChangeTransactionHandler.class);
//								PendingCommandFactory.Util.executeSequence(collector, pendingCommands);
								collector.execute(pendingCommands);
								collector.commitTransaction();
							}
						});
					} else {
						// Moving within same canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);
						
						collector.execute(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
								ArrayList<Object> pendingCommands = new ArrayList<Object>();
								
//								pendingCommands.add(new PendingCommandState<Model>(
//									new SetPropertyCommand("X", new Fraction(droppedBounds.x)),
//									new SetPropertyCommand.AfterSetProperty()
//								));
//								pendingCommands.add(new PendingCommandState<Model>(
//									new SetPropertyCommand("Y", new Fraction(droppedBounds.y)),
//									new SetPropertyCommand.AfterSetProperty()
//								));
								
								pendingCommands.add(new TriStatePURCommand<Model>(
									new CommandSequence<Model>(
										collector.createProduceCommand("X"),
										collector.createProduceCommand(new Fraction(droppedBounds.x)),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									),
									new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
									new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
								));
								
								pendingCommands.add(new TriStatePURCommand<Model>(
									new CommandSequence<Model>(
										collector.createProduceCommand("Y"),
										collector.createProduceCommand(new Fraction(droppedBounds.y)),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									),
									new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
									new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
								));

								collector.startTransaction(selection.getModelBehind(), NewChangeTransactionHandler.class);
//								PendingCommandFactory.Util.executeSequence(collector, pendingCommands);
								collector.execute(pendingCommands);
								collector.commitTransaction();
							}
						});
					}
				} else {
					// Changing bounds within the same canvas
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);
					
					collector.execute(new Trigger<Model>() {
						@Override
						public void run(Collector<Model> collector) {
							ArrayList<Object> pendingCommands = new ArrayList<Object>();

							appendCommandStatesForResize(pendingCommands, selection, newBounds, collector);
							
//							PendingCommandFactory.Util.executeSequence(collector, pendingCommands);
							collector.execute(pendingCommands);
						}
					});
				}
				
				collector.commitTransaction();
			} else {
				collector.rejectTransaction();
			}
		}
	}

	protected abstract void appendCommandStatesForResize(List<Object> commandStates, ModelComponent selection, Rectangle newBounds, Collector<Model> collector);
	
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
			collector.startTransaction(targetModelComponent.getModelBehind(), NewChangeTransactionHandler.class);
			
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
			collector.rejectTransaction();
		}
	}
}
