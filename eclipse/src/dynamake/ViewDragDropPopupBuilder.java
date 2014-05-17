package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Appliance", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				selection.getTransactionFactory().executeOnRoot(
					propCtx, new SetViewTransaction(selection.getTransactionFactory().getLocation(), Model.VIEW_APPLIANCE)
				);
			}
		});
		transactionTargetContentMapBuilder.addTransaction("Engineering", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				selection.getTransactionFactory().executeOnRoot(
					propCtx, new SetViewTransaction(selection.getTransactionFactory().getLocation(), Model.VIEW_ENGINEERING)
				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
	}
}
