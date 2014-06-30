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
import dynamake.models.PropogationContext;
import dynamake.models.ModelTranscriber;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.numbers.Fraction;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;

public class EditTool implements Tool {
	@Override
	public String getName() {
		return "Edit";
	}
	
	@Override
	public void mouseMoved(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(productionPanel.editPanelMouseAdapter.selection == modelOver && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			Point point = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), productionPanel.selectionFrame);
			relativePosition = new RelativePosition(point, ((JComponent)modelOver).getSize());
			productionPanel.selectionFrame.setCursor(relativePosition.getCursor());
		}
	}

	@Override
	public void mouseExited(final ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			
			final TranscriberBranch<Model> branchStep2 = branch.branch();
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			branch.close();

			ModelComponent newTargetOver = targetPresenter.getTargetOver();
			
			targetPresenter.reset(branchStep2);
			targetPresenter = null;
			
			final ModelComponent selection = interactionPresenter.getSelection();
			
			if(!interactionPresenter.getSelectionFrameBounds().equals(interactionPresenter.getEffectFrameBounds())) {
				final ModelTranscriber selectionModelTranscriber = selection.getModelTranscriber();
				if(relativePosition.isInCenter() &&
					newTargetOver.getModelTranscriber() != selection.getModelTranscriber().getParent()) {
					// Moving to other canvas
					final Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, interactionPresenter.getEffectFrameBounds(), (JComponent)newTargetOver);

					final ModelComponent targetOver = newTargetOver;
					
					branchStep2.execute(new PropogationContext(), new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							CanvasModel.appendMoveTransaction(dualCommands, productionPanel.livePanel, selection, targetOver, droppedBounds.getLocation());
						}
					});
				} else {
					// Changing bounds within the same canvas
					JComponent parent = (JComponent)((JComponent)selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, interactionPresenter.getEffectFrameBounds(), parent);
					
					PropogationContext propCtx = new PropogationContext();
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Model selectionModel = selection.getModelBehind();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", new Fraction(newBounds.x)), 
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", selectionModel.getProperty("X"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", new Fraction(newBounds.y)), 
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", selectionModel.getProperty("Y"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", new Fraction(newBounds.width)), 
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", selectionModel.getProperty("Width"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", new Fraction(newBounds.height)), 
								new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", selectionModel.getProperty("Height"))
							));
						}
					});
				}
			}

			interactionPresenter.reset(branchStep2);
			
			productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
			branchStep2.close();
			
			mouseDown = null;
		}
	}
	
	private Point mouseDown;
	private ModelComponent viewPressedOn;
	private TranscriberBranch<Model> branch;
	private RelativePosition relativePosition;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			branch = productionPanel.livePanel.getModelTranscriber().createBranch();
			TranscriberBranch<Model> branchStep1 = branch.branch();
			branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, branchStep1);
			
			relativePosition = new RelativePosition(referencePoint, ((JComponent)targetModelComponent).getSize());
			final Cursor cursor = relativePosition.getCursor();
			
			branchStep1.onFinished(new Runnable() {
				@Override
				public void run() {
					interactionPresenter.setSelectionFrameCursor(cursor);
					interactionPresenter.setEffectFrameCursor(cursor);
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
			targetPresenter.update(newTargetOver, branchStep1);
			
			branchStep1.close();
			
			mouseDown = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, interactionPresenter.getSelection());
			targetPresenter.update(newTargetOver, runBuilder);
			
			Rectangle newEffectBounds = relativePosition.resize(
				interactionPresenter.getSelectionFrameLocation(), 
				interactionPresenter.getSelectionFrameSize(), 
				mouseDown, 
				interactionPresenter.getEffectFrameBounds(), 
				e.getPoint());
			
			interactionPresenter.changeEffectFrameDirect2(newEffectBounds, runBuilder);
			
			runBuilder.execute();
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
}
