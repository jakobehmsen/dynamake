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

import org.prevayler.Transaction;

import dynamake.CanvasModel.MoveModelTransaction;
import dynamake.CanvasModel.SetOutputMoveModelTransaction;
import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class EditTool implements Tool {
	@Override
	public String getName() {
		return "Edit";
	}
	
	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			Point point = e.getPoint();
			
			productionPanel.editPanelMouseAdapter.updateRelativeCursorPosition(point, productionPanel.effectFrame.getSize());
			
			Cursor cursor = null;
			
			switch(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_WEST:
				switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
					cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
					break;
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
					cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
					break;
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
					cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
					break;
				}
				break;
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
				switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
					cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
					break;
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
					cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
					break;
				}
				break;
			case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST:
				switch(productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition) {
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
					cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
					break;
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
					cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
					break;
				case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
					cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
					break;
				}
				break;
			}
			
			if(productionPanel.effectFrame.getCursor() != cursor) {
				productionPanel.effectFrame.setCursor(cursor);
			}
		}
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown == null) {
			productionPanel.effectFrame.setCursor(null);
		}
	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
		if(viewPressedOn != null) {
			viewPressedOn = null;
			productionPanel.editPanelMouseAdapter.selectionMouseDown = null;
			
			final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			branch.close();
			
			if(!productionPanel.selectionFrame.getBounds().equals(productionPanel.effectFrame.getBounds())) {
				final TransactionFactory selectionTransactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
				if(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.targetOver.getTransactionFactory() != productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent()) {
					// Moving to other canvas

					final Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, productionPanel.effectFrame.getBounds(), (JComponent)productionPanel.editPanelMouseAdapter.targetOver);

					final ModelComponent selection = productionPanel.editPanelMouseAdapter.selection;
					final ModelComponent targetOver = productionPanel.editPanelMouseAdapter.targetOver;
					
					branchStep2.execute(new PropogationContext(), new DualCommandFactory<Model>() {
						public DualCommand<Model> createDualCommand() {
							Location livePanelLocation = productionPanel.livePanel.getTransactionFactory().getModelLocation();
							Location canvasSourceLocation = selection.getTransactionFactory().getParent().getModelLocation();
							Location canvasTargetLocation = targetOver.getTransactionFactory().getModelLocation();
							Location modelLocation = selection.getTransactionFactory().getModelLocation();
							
							int indexTarget = ((CanvasModel)targetOver.getModelBehind()).getModelCount();
							CanvasModel sourceCanvas = (CanvasModel)ModelComponent.Util.getParent(selection).getModelBehind();
							int indexSource = sourceCanvas.indexOfModel(selection.getModelBehind());
							CanvasModel targetCanvas = (CanvasModel)targetOver.getModelBehind();
							
							Location canvasTargetLocationAfter;
							int indexOfTargetCanvasInSource = sourceCanvas.indexOfModel(targetCanvas);
							if(indexOfTargetCanvasInSource != -1 && indexSource < indexOfTargetCanvasInSource) {
								// If target canvas is contained with the source canvas, then special care needs to be taken as
								// to predicting the location of target canvas after the move has taken place:
								// - If index of target canvas > index of model to be moved, then the predicated index of target canvas should 1 less
								int predictedIndexOfTargetCanvasInSource = indexOfTargetCanvasInSource - 1;
								canvasTargetLocationAfter = selectionTransactionFactory.getParent().extendLocation(new CanvasModel.IndexLocation(predictedIndexOfTargetCanvasInSource));
							} else {
								canvasTargetLocationAfter = canvasTargetLocation;
							}
							
							Fraction x = (Fraction)selection.getModelBehind().getProperty("X");
							Fraction y = (Fraction)selection.getModelBehind().getProperty("Y");
							
							return new DualCommandPair<Model>(
								new MoveModelTransaction(livePanelLocation, canvasSourceLocation, canvasTargetLocation, modelLocation, droppedBounds.getLocation(), true), 
								new SetOutputMoveModelTransaction(livePanelLocation, canvasTargetLocationAfter, canvasSourceLocation, indexTarget, indexSource, x, y));
						}
						
						@Override
						public void createDualCommands(
								List<DualCommand<Model>> dualCommands) {
							dualCommands.add(createDualCommand());
						}
					});
				} else {
					// Changing bounds within the same canvas
					
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					final Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel.effectFrame.getParent(), productionPanel.effectFrame.getBounds(), parent);
					
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
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "X", new Fraction(newBounds.x)), 
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "X", selectionModel.getProperty("X"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Y", new Fraction(newBounds.y)), 
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Y", selectionModel.getProperty("Y"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Width", new Fraction(newBounds.width)), 
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Width", selectionModel.getProperty("Width"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Height", new Fraction(newBounds.height)), 
								new Model.SetPropertyOnRootTransaction(selectionTransactionFactory.getModelLocation(), "Height", selectionModel.getProperty("Height"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.EndedUpdateTransaction(selectionTransactionFactory.getModelLocation()), 
								new Model.BeganUpdateTransaction(selectionTransactionFactory.getModelLocation())
							));
						}
					});
					
//					productionPanel.livePanel.getTransactionFactory().executeOnRoot(new PropogationContext(), new Model.SetPropertyOnRootTransaction(
//						selectionTransactionFactory.getModelLocation(), 
//						"SelectionEffectBounds", 
//						productionPanel.effectFrame.getBounds())
//					);
					
					// Let the effect be transient only from now on?
					productionPanel.editPanelMouseAdapter.resetEffectFrame();
				}
				
				productionPanel.editPanelMouseAdapter.targetOver = null;
				productionPanel.editPanelMouseAdapter.clearTarget();
			}
			
			branchStep2.close();
			
//			PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
//			connection.commit(propCtx);
		}
	}
	
	private ModelComponent viewPressedOn;
//	private PrevaylerServiceConnection<Model> connection;
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e) {
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent =  productionPanel.editPanelMouseAdapter.closestModelComponent(target);

		if(e.getButton() == MouseEvent.BUTTON1 && targetModelComponent != productionPanel.contentView.getBindingTarget()) {
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
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true, branchStep1);
				productionPanel.livePanel.repaint();
				
				branchStep1.close();
			}
		}
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null && productionPanel.editPanelMouseAdapter.effectFrameMoving && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			
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
			
			
			int x = productionPanel.effectFrame.getX();
			int y = productionPanel.effectFrame.getY();
			int width = productionPanel.effectFrame.getWidth();
			int height = productionPanel.effectFrame.getHeight();
			
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

			productionPanel.effectFrame.setBounds(new Rectangle(x, y, width, height));
			productionPanel.livePanel.repaint();
		}
	}
}
