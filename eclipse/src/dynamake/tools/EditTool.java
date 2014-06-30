package dynamake.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.Fraction;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.TransactionFactory;
import dynamake.models.LiveModel.ProductionPanel;
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
			
			if(!productionPanel.selectionFrame.getBounds().equals(productionPanel.editPanelMouseAdapter.getEffectFrameBounds())) {
				final TransactionFactory selectionTransactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
				if(relativePosition.isInCenter() &&
					newTargetOver.getTransactionFactory() != productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent()) {
					// Moving to other canvas
					final Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), (JComponent)newTargetOver);

					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					final ModelComponent targetOver = newTargetOver;
					
					branchStep2.execute(new PropogationContext(), new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							CanvasModel.appendMoveTransaction(dualCommands, productionPanel.livePanel, selection, targetOver, droppedBounds.getLocation());
						}
					});
				} else {
					// Changing bounds within the same canvas
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), parent);
					
					PropogationContext propCtx = new PropogationContext();
					
					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Model selectionModel = selection.getModelBehind();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "X", new Fraction(newBounds.x)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "X", selectionModel.getProperty("X"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Y", new Fraction(newBounds.y)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Y", selectionModel.getProperty("Y"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Width", new Fraction(newBounds.width)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Width", selectionModel.getProperty("Width"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Height", new Fraction(newBounds.height)), 
								new Model.SetPropertyTransaction(selectionTransactionFactory.getModelLocation(), "Height", selectionModel.getProperty("Height"))
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, selectionTransactionFactory.getModelLocation()));
						}
					});
				}
			} else {
				PropogationContext propCtx = new PropogationContext();
				
				final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
				
				branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, selection.getTransactionFactory().getModelLocation()));
					}
				});
			}
			
			final Cursor cursor = relativePosition.getCursor();
			final JPanel localSelectionFrame = productionPanel.selectionFrame;
			
			branchStep2.onFinished(new Runnable() {
				@Override
				public void run() {
					localSelectionFrame.setCursor(cursor);
				}
			});
			
			productionPanel.editPanelMouseAdapter.setEffectFrameCursor2(null, branchStep2);
			
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

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			TranscriberBranch<Model> branchStep1 = branch.branch();
			branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
			relativePosition = new RelativePosition(referencePoint, ((JComponent)targetModelComponent).getSize());
			final Cursor cursor = relativePosition.getCursor();
			
			branchStep1.onFinished(new Runnable() {
				@Override
				public void run() {
					productionPanel.selectionFrame.setCursor(cursor);
					productionPanel.editPanelMouseAdapter.setEffectFrameCursor(cursor);
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
			
			ModelComponent newTargetOver = getTargetOver(productionPanel, modelOver, productionPanel.editPanelMouseAdapter.selection);
			targetPresenter.update(newTargetOver, runBuilder);
			
			Rectangle newEffectBounds = relativePosition.resize(
				productionPanel.selectionFrame.getLocation(), 
				productionPanel.selectionFrame.getSize(), 
				mouseDown, 
				productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), 
				e.getPoint());
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(newEffectBounds, runBuilder);
			
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
		// TODO Auto-generated method stub
		
	}
}