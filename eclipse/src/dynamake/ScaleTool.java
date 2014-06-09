package dynamake;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.CanvasModel.MoveModelTransaction;
import dynamake.CanvasModel.SetOutputMoveModelTransaction;
import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;
import dynamake.Model.SetPropertyTransaction;

public class ScaleTool implements Tool {
	@Override
	public String getName() {
		return "Scale";
	}
	
	@Override
	public void mouseMoved(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			Point point = e.getPoint();
			
			productionPanel.editPanelMouseAdapter.updateRelativeCursorPosition(point, productionPanel.selectionFrame.getSize());
			
			final Cursor cursor = productionPanel.editPanelMouseAdapter.getCursorFromRelativePosition();
			
			if(productionPanel.selectionFrame.getCursor() != cursor) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						productionPanel.selectionFrame.setCursor(cursor);
					}
				});
			}
		}
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown == null) {
			productionPanel.selectionFrame.setCursor(null);
		}
	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
//		if(e.getButton() == MouseEvent.BUTTON1 && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			productionPanel.editPanelMouseAdapter.selectionMouseDown = null;
			
			final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			branch.close();
			
			if(!productionPanel.selectionFrame.getBounds().equals(productionPanel.editPanelMouseAdapter.getEffectFrameBounds())) {
				final TransactionFactory selectionTransactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
				if(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.targetOver.getTransactionFactory() != productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent()) {
					// Moving to other canvas

					final Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), (JComponent)productionPanel.editPanelMouseAdapter.targetOver);

					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					final ModelComponent targetOver = productionPanel.editPanelMouseAdapter.targetOver;
					
					branchStep2.execute(new PropogationContext(), new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(
								List<DualCommand<Model>> dualCommands) {
							CanvasModel.appendMoveTransaction(dualCommands, productionPanel.livePanel, selection, targetOver, droppedBounds.getLocation());
						}
					});
				} else {
					// Changing bounds within the same canvas
					
					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), parent);
					
					PropogationContext propCtx = new PropogationContext();
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							selection.getModelBehind().appendScale(newBounds, dualCommands);
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, selectionTransactionFactory.getModelLocation())); // Absolute location
						}
					});
				}
				
				productionPanel.editPanelMouseAdapter.targetOver = null;
				productionPanel.editPanelMouseAdapter.clearTarget();
			}
			
			productionPanel.editPanelMouseAdapter.clearEffectFrame();
			branchStep2.close();
		}
	}
	
	private ModelComponent viewPressedOn;
	private PrevaylerServiceBranch<Model> branch;
	
	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e) {
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent =  productionPanel.editPanelMouseAdapter.closestModelComponent(target);
		
		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			if(targetModelComponent != null) {
				viewPressedOn = targetModelComponent;
				branch = productionPanel.livePanel.getTransactionFactory().createBranch();
//				connection = productionPanel.livePanel.getTransactionFactory().createConnection();
				PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
				
				if(productionPanel.editPanelMouseAdapter.output != null) {
					PropogationContext propCtx = new PropogationContext();
					
					branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
						public DualCommand<Model> createDualCommand() {
							ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
							return new DualCommandPair<Model>(
								new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
								new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
							);
						}
						
						@Override
						public void createDualCommands(
								List<DualCommand<Model>> dualCommands) {
							dualCommands.add(createDualCommand());
						}
					});
				}
				
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
				productionPanel.editPanelMouseAdapter.updateRelativeCursorPosition(referencePoint, ((JComponent)targetModelComponent).getSize());
				if(productionPanel.selectionFrame != null)
					productionPanel.editPanelMouseAdapter.setEffectFrameCursor(productionPanel.selectionFrame.getCursor());
				
				branchStep1.close();
			}
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			ModelComponent newTargetOverComponent;
			
			if(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
			   productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER) {
				// Moving
				Point mouseOverPoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
				JComponent newTargetOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
				newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(newTargetOver);
				
				if(((JComponent)productionPanel.editPanelMouseAdapter.selection).isAncestorOf((JComponent)newTargetOverComponent))
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.selection;
				
				if(newTargetOverComponent == productionPanel.editPanelMouseAdapter.selection) {
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(((JComponent)newTargetOverComponent).getParent());
				}
			} else {
				// Scaling
				newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent());
			}
			
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null)
					productionPanel.remove(productionPanel.targetFrame);
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					
					Color color = ProductionPanel.TARGET_OVER_COLOR;

					productionPanel.targetFrame.setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(Color.BLACK, 1), 
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(color, 3), 
								BorderFactory.createLineBorder(Color.BLACK, 1)
							)
						)
					);
					
					Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
						((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), productionPanel);
					productionPanel.targetFrame.setBounds(targetFrameBounds);
					productionPanel.targetFrame.setBackground(new Color(0, 0, 0, 0));
					productionPanel.add(productionPanel.targetFrame);
				}
			}

			int x = productionPanel.editPanelMouseAdapter.getEffectFrameX();
			int y = productionPanel.editPanelMouseAdapter.getEffectFrameY();
			int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();
			
			Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			
			switch(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_WEST: {
				int currentX = x;
				x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
				width += currentX - x;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST: {
				width = productionPanel.editPanelMouseAdapter.selectionFrameSize.width + e.getX() - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
				switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
					x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
					y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;
					break;
				}
				break;
			}
			
			switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH: {
				int currentY = y;
				y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;
				height += currentY - y;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH: {
				height = productionPanel.editPanelMouseAdapter.selectionFrameSize.height + e.getY() - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;
				
				break;
			}
			}
		
			final Rectangle newEffectBounds = new Rectangle(x, y, width, height);

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.editPanelMouseAdapter.changeEffectFrameDirect(newEffectBounds);
					productionPanel.livePanel.repaint();
				}
			});
		}
	}
}
