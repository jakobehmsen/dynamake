package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Tell Background", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_BACKGROUND)
				);
			}
		});
		transactionTargetContentMapBuilder.addTransaction("Tell Foreground", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_FOREGROUND)
				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
	}
}
