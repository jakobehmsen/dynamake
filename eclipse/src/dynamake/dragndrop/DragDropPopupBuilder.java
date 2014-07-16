package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;

public interface DragDropPopupBuilder {
	void buildFromSelectionAndTarget(
		ModelComponent livePanel, JPopupMenu popup,
		ModelComponent selection, ModelComponent target,
		Point dropPointOnTarget, Rectangle dropBoundsOnTarget);
	void cancelPopup(LivePanel livePanel);
}
