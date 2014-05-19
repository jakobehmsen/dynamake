package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public interface DragDropPopupBuilder {
	void buildFromSelectionAndTarget(
		Runner runner, ModelComponent livePanel,
		JPopupMenu popup, ModelComponent selection,
		ModelComponent target, Point dropPointOnTarget, Rectangle dropBoundsOnTarget);
}
