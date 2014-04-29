package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public interface DragDropPopupBuilder {
	void buildFromSelectionAndTarget(JPopupMenu popup, ModelComponent selection,
			ModelComponent target, Point dropPointOnTarget,
			Rectangle dropBoundsOnTarget);
	
	void buildFromSelectionToSelection(JPopupMenu popup, ModelComponent selection);
	void buildFromSelectionToOther(JPopupMenu popup, ModelComponent selection,
			ModelComponent target, Point dropPointOnTarget,
			Rectangle dropBoundsOnTarget);
}
