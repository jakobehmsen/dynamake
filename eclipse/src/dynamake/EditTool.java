package dynamake;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.prevayler.Transaction;

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
			
			TransactionFactory transactionFactory = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory();
			
			JComponent parent = (JComponent)((JComponent)productionPanel.editPanelMouseAdapter.selection).getParent();
			Rectangle newBounds = SwingUtilities.convertRectangle(productionPanel.effectFrame.getParent(), productionPanel.effectFrame.getBounds(), parent);
			productionPanel.selectionFrame.setBounds(productionPanel.effectFrame.getBounds());
			productionPanel.livePanel.repaint();
			
			@SuppressWarnings("unchecked")
			Transaction<Model> changeBoundsTransaction = new Model.CompositeTransaction((Transaction<Model>[])new Transaction<?>[] {
				new Model.SetPropertyTransaction("X", new Fraction(newBounds.x)),
				new Model.SetPropertyTransaction("Y", new Fraction(newBounds.y)),
				new Model.SetPropertyTransaction("Width", new Fraction(newBounds.width)),
				new Model.SetPropertyTransaction("Height", new Fraction(newBounds.height))
			});
			transactionFactory.execute(changeBoundsTransaction);
			
			productionPanel.livePanel.getTransactionFactory().execute(new Model.SetPropertyTransaction("SelectionEffectBounds", productionPanel.effectFrame.getBounds()));
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
