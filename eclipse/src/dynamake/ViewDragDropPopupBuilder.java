package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target,
			final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Appliance", new Runnable() {
			@Override
			public void run() {
//				selection.getTransactionFactory().executeOnRoot(
//					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_BACKGROUND)
//				);
			}
		});
		transactionTargetContentMapBuilder.addTransaction("Engineering", new Runnable() {
			@Override
			public void run() {
//				selection.getTransactionFactory().executeOnRoot(
//					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_FOREGROUND)
//				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
	}
}
