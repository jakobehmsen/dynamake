package dynamake.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.numbers.Fraction;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberOnFlush;

public abstract class BoundsChangeTool implements Tool {
	@Override
	public void mouseMoved(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
//		if(productionPanel.editPanelMouseAdapter.selection == modelOver && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
//			Point point = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), productionPanel.selectionFrame);
//			relativePosition = new RelativePosition(point, ((JComponent)modelOver).getSize());
//			productionPanel.selectionFrame.setCursor(relativePosition.getCursor());
//		}
	}

	@Override
	public void mouseExited(final ProductionPanel productionPanel, MouseEvent e, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			
//			final TranscriberBranch<Model> branchStep2 = branch.branch();
//			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
//			branch.close();

			ModelComponent newTargetOver = targetPresenter.getTargetOver();
			
			targetPresenter.reset(collector);
			targetPresenter = null;
			
			final ModelComponent selection = interactionPresenter.getSelection();
			
			if(!interactionPresenter.getSelectionFrameBounds().equals(interactionPresenter.getEffectFrameBounds())) {
				if(relativePosition.isInCenter()) {
					if(newTargetOver.getModelTranscriber() != selection.getModelTranscriber().getParent()) {
						// Moving to other canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(
							productionPanel, interactionPresenter.getEffectFrameBounds(), (JComponent)newTargetOver);

						final ModelComponent targetOver = newTargetOver;
						
						collector.enqueue(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								CanvasModel.appendMoveTransaction(dualCommands, productionPanel.livePanel, selection, targetOver, droppedBounds.getLocation());
							}
						});
					} else {
						// Moving within same canvas
						final Rectangle droppedBounds = SwingUtilities.convertRectangle(productionPanel, interactionPresenter.getEffectFrameBounds(), (JComponent)newTargetOver);
						
						collector.enqueue(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), "X", new Fraction(droppedBounds.x)), 
									new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), "X", selection.getModelBehind().getProperty("X"))
								));
								dualCommands.add(new DualCommandPair<Model>(
									new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), "Y", new Fraction(droppedBounds.y)), 
									new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), "Y", selection.getModelBehind().getProperty("Y"))
								));
							}
						});
					}
				} else {
					// Changing bounds within the same canvas
					JComponent parent = (JComponent)((JComponent)selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, interactionPresenter.getEffectFrameBounds(), parent);
					
//					PropogationContext propCtx = new PropogationContext();
					
					collector.enqueue(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							appendDualCommandsForResize(dualCommands, selection, newBounds);
						}
					});
				}

				interactionPresenter.reset(collector);
				interactionPresenter = null;
				
				collector.commit();
			} else {
				interactionPresenter.reset(collector);
				interactionPresenter = null;
				
				collector.reject();
			}
			
//			branchStep2.close();
			
			mouseDown = null;
		}
	}

	protected abstract void appendDualCommandsForResize(List<DualCommand<Model>> dualCommands, ModelComponent selection, Rectangle newBounds);
	
	private Point mouseDown;
	private ModelComponent viewPressedOn;
//	private TranscriberBranch<Model> branch;
	private RelativePosition relativePosition;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
//			branch = productionPanel.livePanel.getModelTranscriber().createBranch();
//			TranscriberBranch<Model> branchStep1 = branch.branch();
//			branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
			
			relativePosition = new RelativePosition(referencePoint, ((JComponent)targetModelComponent).getSize());
			final Cursor cursor = relativePosition.getCursor();
			
			final InteractionPresenter locationInteractionPresenter = interactionPresenter;
			collector.afterNextFlush(new TranscriberOnFlush<Model>() {
				@Override
				public void run(TranscriberCollector<Model> collector) {
					locationInteractionPresenter.setSelectionFrameCursor(cursor);
					locationInteractionPresenter.setEffectFrameCursor(cursor);
				}
			});
//			branchStep1.onFinished(new Runnable() {
//				@Override
//				public void run() {
//					locationInteractionPresenter.setSelectionFrameCursor(cursor);
//					locationInteractionPresenter.setEffectFrameCursor(cursor);
//				}
//			});
			
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
			
//			branchStep1.close();
			
			mouseDown = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		if(mouseDown != null && interactionPresenter.getSelection() != productionPanel.contentView.getBindingTarget()) {
//			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, interactionPresenter.getSelection());
			targetPresenter.update(newTargetOver, collector);
			
			Rectangle newEffectBounds = relativePosition.resize(
				interactionPresenter.getSelectionFrameLocation(), 
				interactionPresenter.getSelectionFrameSize(), 
				mouseDown, 
				interactionPresenter.getEffectFrameBounds(), 
				e.getPoint());
			
			interactionPresenter.changeEffectFrameDirect2(newEffectBounds, collector);
			
//			runBuilder.execute();
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
			newTargetOver = ModelComponent.Util.closestModelComponent(((JComponent)selection).getParent());
		}
		
		newTargetOver = ModelComponent.Util.closestCanvasModelComponent(newTargetOver);
		
		return newTargetOver;
	}

	@Override
	public void paint(Graphics g) {

	}
	
	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		if(mouseDown != null) {
//			final TranscriberBranch<Model> branchStep2 = branch.branch();
//			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			targetPresenter.reset(collector);
			targetPresenter = null;
	
			interactionPresenter.reset(collector);
			interactionPresenter = null;
			
//			branchStep2.close();
//			
//			branch.reject();
		}
	}
}
