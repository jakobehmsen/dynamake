package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(Runner runner,
			ModelComponent livePanel, JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Tell Color", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				selection.getTransactionFactory().executeOnRoot(
					propCtx, new TellPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_COLOR)
				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
	}
}
