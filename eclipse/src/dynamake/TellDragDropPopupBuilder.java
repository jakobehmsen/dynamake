package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target,
			final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Tell Background", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
//					new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_BACKGROUND)
				);
			}
		});
		transactionTargetContentMapBuilder.addTransaction("Tell Foreground", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
//					new Model.AddObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
					new TellPropertyTransaction(selection.getTransactionFactory().getLocation(), Model.PROPERTY_FOREGROUND)
				);
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
		
//		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
//			transactionTargetContentMapBuilder.addTransaction("Tell Background", new Runnable() {
//				@Override
//				public void run() {
//					selection.getTransactionFactory().executeOnRoot(
//						new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
//					);
//				}
//			});
//		} else {
//			transactionTargetContentMapBuilder.addTransaction("Tell Foregraound", new Runnable() {
//				@Override
//				public void run() {
//					selection.getTransactionFactory().executeOnRoot(
//						new Model.AddObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
//					);
//				}
//			});
//		}
//		transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
//		popup.addSeparator();
//		
//		TransactionMapBuilder transactionObserverContentMapBuilder = new TransactionMapBuilder();
//		for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
//			final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
//			transactionObserverContentMapBuilder.addTransaction(primImpl.getName(), new Runnable() {
//				@Override
//				public void run() {
//					target.getTransactionFactory().executeOnRoot(new AddThenBindTransaction(
//						selection.getTransactionFactory().getLocation(), 
//						target.getTransactionFactory().getLocation(), 
//						new PrimitiveSingletonFactory(primImpl), 
//						dropBoundsOnTarget
//					));
//				}
//			});
//		}
//		transactionObserverContentMapBuilder.appendTo(popup, "Observation");
	}

	@Override
	public void buildFromSelectionToSelection(JPopupMenu popup,
			ModelComponent selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void buildFromSelectionToOther(JPopupMenu popup,
			ModelComponent selection, ModelComponent target,
			Point dropPointOnTarget, Rectangle dropBoundsOnTarget) {
		// TODO Auto-generated method stub

	}
}
