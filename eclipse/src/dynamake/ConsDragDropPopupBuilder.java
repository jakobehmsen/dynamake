package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import org.prevayler.Transaction;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(Runner runner,
			final ModelComponent livePanel, JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target, Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Command<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			selection.getTransactionFactory().executeOnRoot(new PropogationContext(), implicitDropAction);
		} else {
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addTransaction("Unforward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new PropogationContext(), new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
						);
					}
				});
			} else {
				transactionTargetContentMapBuilder.addTransaction("Forward to", new Runnable() {
					@Override
					public void run() {
						selection.getTransactionFactory().executeOnRoot(
							new PropogationContext(), new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
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
						target.getTransactionFactory().executeOnRoot(new PropogationContext(), new AddThenBindAndOutputTransaction(
							livePanel.getTransactionFactory().getModelLocation(),
							selection.getTransactionFactory().getModelLocation(), 
							target.getTransactionFactory().getModelLocation(), 
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
