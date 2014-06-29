package dynamake;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import dynamake.models.LiveModel.ProductionPanel;

public class RelativePosition {
	public static final int HORIZONTAL_REGION_WEST = 0;
	public static final int HORIZONTAL_REGION_CENTER = 1;
	public static final int HORIZONTAL_REGION_EAST = 2;
	public static final int VERTICAL_REGION_NORTH = 0;
	public static final int VERTICAL_REGION_CENTER = 1;
	public static final int VERTICAL_REGION_SOUTH = 2;
	
	public final int horizontalPosition;
	public final int verticalPosition;
	
	public RelativePosition(Point point, Dimension size) {
		int resizeWidth = 5;
		
		int leftPositionEnd = resizeWidth;
		int rightPositionStart = size.width - resizeWidth;

		int topPositionEnd = resizeWidth;
		int bottomPositionStart = size.height - resizeWidth;
		
		if(point.x <= leftPositionEnd)
			horizontalPosition = HORIZONTAL_REGION_WEST;
		else if(point.x < rightPositionStart)
			horizontalPosition = HORIZONTAL_REGION_CENTER;
		else
			horizontalPosition = HORIZONTAL_REGION_EAST;
		
		if(point.y <= topPositionEnd)
			verticalPosition = VERTICAL_REGION_NORTH;
		else if(point.y < bottomPositionStart)
			verticalPosition = VERTICAL_REGION_CENTER;
		else
			verticalPosition = VERTICAL_REGION_SOUTH;
	}
	
	public Cursor getCursor() {
		switch(horizontalPosition) {
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_WEST:
			switch(verticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
				return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
				return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
				return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
			default:
				return null;
			}
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
			switch(verticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
				return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
				return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
			default:
				return null;
			}
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST:
			switch(verticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH:
				return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
				return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH:
				return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
			default:
				return null;
			}
		default:
			return null;
		}
	}

	public boolean isInCenter() {
		return horizontalPosition == HORIZONTAL_REGION_CENTER && verticalPosition == VERTICAL_REGION_CENTER;
	}

	public Rectangle resize(Point offset, Dimension sizeDown, Point mouseDown, Rectangle rectangleCurrent, Point mouseCurrent) {
		int x = rectangleCurrent.x;
		int y = rectangleCurrent.y;
		int width = rectangleCurrent.width;
		int height = rectangleCurrent.height;
		
		switch(horizontalPosition) {
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_WEST: {
			int currentX = x;
			x = offset.x + mouseCurrent.x - mouseDown.x;
//			System.out.println("mouseCurrent=" + mouseCurrent + ",mouseDown=" + mouseDown);
			width += currentX - x;
//			System.out.println("mouseCurrent=" + mouseCurrent + ",mouseDown=" + mouseDown + ",x=" + x + ",currentX=" + currentX + ",width=" + width);
			
			break;
		}
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_EAST: {
			width = sizeDown.width + mouseCurrent.x - mouseDown.x;
			
			break;
		}
		case ProductionPanel.EditPanelMouseAdapter.HORIZONTAL_REGION_CENTER:
			switch(verticalPosition) {
			case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_CENTER:
				x = offset.x + mouseCurrent.x - mouseDown.x;
				y = offset.y + mouseCurrent.y - mouseDown.y;
				break;
			}
			break;
		}
		
		switch(verticalPosition) {
		case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_NORTH: {
			int currentY = y;
			y = offset.y + mouseCurrent.y - mouseDown.y;
			height += currentY - y;
			
			break;
		}
		case ProductionPanel.EditPanelMouseAdapter.VERTICAL_REGION_SOUTH: {
			height = sizeDown.height + mouseCurrent.y - mouseDown.y;
			
			break;
		}
		}
		
		return new Rectangle(x, y, width, height);
	}
}
