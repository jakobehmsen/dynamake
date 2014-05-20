package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(Runner runner,
			ModelComponent livePanel, JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Appliance", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				selection.getTransactionFactory().executeOnRoot(
					propCtx, new SetViewTransaction(selection.getTransactionFactory().getModelLocation(), Model.VIEW_APPLIANCE)
				);
			}
		});
		transactionTargetContentMapBuilder.addTransaction("Engineering", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				selection.getTransactionFactory().executeOnRoot(
					propCtx, new SetViewTransaction(selection.getTransactionFactory().getModelLocation(), Model.VIEW_ENGINEERING)
				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {

	}
}
