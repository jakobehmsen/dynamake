package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public interface DragDropPopupBuilder {
	void buildFromSelectionAndTarget(
		Runner runner, ModelComponent livePanel,
		JPopupMenu popup, ModelComponent selection,
		ModelComponent target, Point dropPointOnTarget, Rectangle dropBoundsOnTarget);

	void cancelPopup(LivePanel livePanel);
}
