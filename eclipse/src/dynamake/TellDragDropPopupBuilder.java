package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	private PrevaylerServiceConnection<Model> connection;
	
	public TellDragDropPopupBuilder(PrevaylerServiceConnection<Model> connection) {
		this.connection = connection;
	}

	@Override
	public void buildFromSelectionAndTarget(ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();
				
				PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
				connection.commit(propCtx);
			}
		};
		
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Tell Color", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				connection.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						dualCommands.add(new DualCommandPair<Model>(
							new TellPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_COLOR),
							null
						));
					}
				});
//				selection.getTransactionFactory().executeOnRoot(
//					propCtx, new TellPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_COLOR)
//				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {
		PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_ROLLBACK);
		connection.rollback(propCtx);
	}
}
