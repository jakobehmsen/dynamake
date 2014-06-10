package dynamake;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class EditTool implements Tool {
	@Override
	public String getName() {
		return "Edit";
	}
	
	@Override
	public void mouseMoved(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(productionPanel.editPanelMouseAdapter.selection == modelOver && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			Point point = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), productionPanel.selectionFrame);
			
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
	public void mouseExited(final ProductionPanel productionPanel, MouseEvent e) {
		if(mouseDown == null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.selectionFrame.setCursor(null);
				}
			});
		}
	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			
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
					
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel, productionPanel.editPanelMouseAdapter.getEffectFrameBounds(), parent);
					
					PropogationContext propCtx = new PropogationContext();
					
					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(
								List<DualCommand<Model>> dualCommands) {
							Model selectionModel = selection.getModelBehind();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.BeganUpdateTransaction(selectionTransactionFactory.getModelLocation()), 
								new Model.EndedUpdateTransaction(selectionTransactionFactory.getModelLocation())
							));
							
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
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.EndedUpdateTransaction(selectionTransactionFactory.getModelLocation()), 
								new Model.BeganUpdateTransaction(selectionTransactionFactory.getModelLocation())
							));
						}
					});
				}
				
				productionPanel.editPanelMouseAdapter.targetOver = null;
				productionPanel.editPanelMouseAdapter.clearTarget();
			}
			
			productionPanel.editPanelMouseAdapter.clearEffectFrame();
			branchStep2.close();
			
			mouseDown = null;
		}
	}
	
	private Point mouseDown;
	private ModelComponent viewPressedOn;
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		ModelComponent targetModelComponent = modelOver;

		if(targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			viewPressedOn = targetModelComponent;
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
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
			
			mouseDown = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			ModelComponent newTargetOverComponent;
			
			if(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
			   productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER) {
				// Moving
				newTargetOverComponent = modelOver;
				
				if(((JComponent)productionPanel.editPanelMouseAdapter.selection).isAncestorOf((JComponent)newTargetOverComponent))
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.selection;
				
				if(newTargetOverComponent == productionPanel.editPanelMouseAdapter.selection) {
					newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(((JComponent)newTargetOverComponent).getParent());
				}
			} else {
				// Resizing
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
				x = cursorLocationInProductionPanel.x - mouseDown.x;
				width += currentX - x;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST: {
				width = productionPanel.editPanelMouseAdapter.selectionFrameSize.width + e.getX() - mouseDown.x;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
				switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
					x = cursorLocationInProductionPanel.x - mouseDown.x;
					y = cursorLocationInProductionPanel.y - mouseDown.y;
					break;
				}
				break;
			}
			
			switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH: {
				int currentY = y;
				y = cursorLocationInProductionPanel.y - mouseDown.y;
				height += currentY - y;
				
				break;
			}
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH: {
				height = productionPanel.editPanelMouseAdapter.selectionFrameSize.height + e.getY() - mouseDown.y;
				
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
