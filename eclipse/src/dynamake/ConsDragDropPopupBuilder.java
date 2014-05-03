package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import org.prevayler.Transaction;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target,
			Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Transaction<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			selection.getTransactionFactory().executeOnRoot(implicitDropAction);
		} else {
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addTransaction("Unforward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
						);
					}
				});
			} else {
				transactionTargetContentMapBuilder.addTransaction("Forward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new Model.AddObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
						);
					}
				});
			}
			transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
			popup.addSeparator();
			
			TransactionMapBuilder transactionObserverContentMapBuilder = new TransactionMapBuilder();
			for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
				final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
				transactionObserverContentMapBuilder.addTransaction(primImpl.getName(), new Runnable() {
					@Override
					public void run() {
						target.getTransactionFactory().executeOnRoot(new AddThenBindTransaction(
							selection.getTransactionFactory().getLocation(), 
							target.getTransactionFactory().getLocation(), 
							new PrimitiveSingletonFactory(primImpl), 
							dropBoundsOnTarget
						));
					}
				});
			}
			transactionObserverContentMapBuilder.appendTo(popup, "Observation");
		}
	}
	
	@Override
	public void buildFromSelectionToSelection(JPopupMenu popup,
			ModelComponent selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void buildFromSelectionToOther(JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target,
			final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Transaction<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			selection.getTransactionFactory().executeOnRoot(implicitDropAction);
		} else {
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addTransaction("Unforward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
						);
					}
				});
			} else {
				transactionTargetContentMapBuilder.addTransaction("Forward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new Model.AddObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
						);
					}
				});
			}
			transactionTargetContentMapBuilder.appendTo(popup, "Selection to target");
			popup.addSeparator();
			
			TransactionMapBuilder transactionObserverContentMapBuilder = new TransactionMapBuilder();
			for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
				final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
				transactionObserverContentMapBuilder.addTransaction(primImpl.getName(), new Runnable() {
					@Override
					public void run() {
						target.getTransactionFactory().executeOnRoot(new AddThenBindTransaction(
							selection.getTransactionFactory().getLocation(), 
							target.getTransactionFactory().getLocation(), 
							new PrimitiveSingletonFactory(primImpl), 
							dropBoundsOnTarget
						));
					}
				});
			}
			transactionObserverContentMapBuilder.appendTo(popup, "Observation");
		}
	}
}
