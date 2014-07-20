package dynamake.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RelativeCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public abstract class BoundsChangeTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
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
						collector.execute(new CommandStateFactory<Model>() {
							ModelComponent referenceMC;
							
							@Override
							public Model getReference() {
								referenceMC = ModelComponent.Util.closestCommonAncestor(source, targetOver);
								return referenceMC.getModelBehind();
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								Location locationOfSource = ModelComponent.Util.locationFromAncestor(referenceMC, source);
								Location locationOfTarget = ModelComponent.Util.locationFromAncestor(referenceMC, targetOver);
								
								CanvasModel.appendMoveTransaction(commandStates, productionPanel.livePanel, source, selection, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget);
							}
						});
					} else {
						// Moving within same canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);

						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return source.getModelBehind();
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								Location locationOfMovedModel = ((CanvasModel)source.getModelBehind()).getLocationOf(selection.getModelBehind());

								commandStates.add(new PendingCommandState<Model>(
									new RelativeCommand<Model>(locationOfMovedModel, new SetPropertyCommand("X", new Fraction(droppedBounds.x))),
									new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
								));
								
								commandStates.add(new PendingCommandState<Model>(
									new RelativeCommand<Model>(locationOfMovedModel, new SetPropertyCommand("Y", new Fraction(droppedBounds.y))),
									new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
								));
							}
						});
					}
				} else {
					// Changing bounds within the same canvas
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, effectBounds, (JComponent)newTargetOver);
					
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return selection.getModelBehind();
						}

						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
							appendCommandStatesForResize(commandStates, selection, newBounds);
						}
					});
				}
				
				collector.commit();
			} else {
				collector.reject();
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
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			source = ModelComponent.Util.getParent(targetModelComponent);
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			
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
			
			mouseDown = e.getPoint();

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
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		if(mouseDown != null && interactionPresenter.getSelection() != productionPanel.contentView.getBindingTarget()) {
			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, interactionPresenter.getSelection());
			targetPresenter.update(newTargetOver, collector);
			
			final Rectangle newEffectBounds = relativePosition.resize(
				interactionPresenter.getSelectionFrameLocation(), 
				interactionPresenter.getSelectionFrameSize(), 
				mouseDown, 
				interactionPresenter.getEffectFrameBounds(), 
				e.getPoint());
			
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
