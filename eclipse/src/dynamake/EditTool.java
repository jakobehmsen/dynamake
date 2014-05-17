package dynamake;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.prevayler.Transaction;

import dynamake.CanvasModel.MoveModelTransaction;
import dynamake.LiveModel.ProductionPanel;

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
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1 && productionPanel.editPanelMouseAdapter.selection != productionPanel.contentView.getBindingTarget()) {
			productionPanel.editPanelMouseAdapter.selectionMouseDown = null;
			
			if(!productionPanel.selectionFrame.getBounds().equals(productionPanel.effectFrame.getBounds())) {
				TransactionFactory transactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
				if(productionPanel.editPanelMouseAdapter.selectionFrameHorizontalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.selectionFrameVerticalPosition == ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER &&
				   productionPanel.editPanelMouseAdapter.targetOver.getTransactionFactory() != productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent()) {
					// Moving to other canvas
					
					Location canvasSourceLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getParent().getLocation();
					Location canvasTargetLocation = productionPanel.editPanelMouseAdapter.targetOver.getTransactionFactory().getLocation();
					Location modelLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getLocation();

					Rectangle droppedBounds = SwingUtilities.convertRectangle(
						productionPanel, productionPanel.effectFrame.getBounds(), (JComponent)productionPanel.editPanelMouseAdapter.targetOver);
					
					transactionFactory.executeOnRoot(new PropogationContext(), new MoveModelTransaction(
						productionPanel.livePanel.getTransactionFactory().getLocation(), 
						canvasSourceLocation, canvasTargetLocation, modelLocation, droppedBounds.getLocation(),
						false
					));
				} else {
					// Changing bounds within the same canvas
					
					JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
					Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel.effectFrame.getParent(), productionPanel.effectFrame.getBounds(), parent);
					
					@SuppressWarnings("unchecked")
					Command<Model> changeBoundsTransaction = new Model.CompositeTransaction((Command<Model>[])new Command<?>[] {
						new Model.SetPropertyTransaction("X", new Fraction(newBounds.x)),
						new Model.SetPropertyTransaction("Y", new Fraction(newBounds.y)),
						new Model.SetPropertyTransaction("Width", new Fraction(newBounds.width)),
						new Model.SetPropertyTransaction("Height", new Fraction(newBounds.height))
					});
					transactionFactory.execute(new PropogationContext(), changeBoundsTransaction);
					
					productionPanel.livePanel.getTransactionFactory().execute(new PropogationContext(), new Model.SetPropertyTransaction("SelectionEffectBounds", productionPanel.effectFrame.getBounds()));
				}
				
				productionPanel.editPanelMouseAdapter.targetOver = null;
				productionPanel.editPanelMouseAdapter.clearTarget();
			}
		}
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e) {
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent =  productionPanel.editPanelMouseAdapter.closestModelComponent(target);
		
		if(e.getButton() == MouseEvent.BUTTON1 && targetModelComponent != productionPanel.contentView.getBindingTarget()) {
			if(targetModelComponent != null) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				 productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true);
				productionPanel.livePanel.repaint();
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
