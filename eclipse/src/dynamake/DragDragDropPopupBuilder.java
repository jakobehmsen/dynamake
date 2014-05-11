package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class DragDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(ModelComponent livePanel,
			JPopupMenu popup, ModelComponent selection,
			ModelComponent target, Point dropPointOnTarget, Rectangle dropBoundsOnTarget) {
		if(target == null || target == selection) {
			// Build popup menu for dropping onto selection
			buildFromSelectionToSelection(popup, selection);
		} else {
			// Build popup menu for dropping onto other
			buildFromSelectionToOther(livePanel, popup, selection, target, dropPointOnTarget, dropBoundsOnTarget);
		}
	}
	
	private void buildFromSelectionToSelection(JPopupMenu popup, ModelComponent selection) {
		ModelComponent parentModelComponent = ModelComponent.Util.closestModelComponent(((JComponent)selection).getParent()); 
		
		TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
		if(parentModelComponent != null)
			parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, selection);

		TransactionMapBuilder transactionSelectionMapBuilder = new TransactionMapBuilder();
		selection.appendTransactions(transactionSelectionMapBuilder);

		containerTransactionMapBuilder.appendTo(popup, "Container");
		if(!containerTransactionMapBuilder.isEmpty() && !transactionSelectionMapBuilder.isEmpty())
			popup.addSeparator();
		transactionSelectionMapBuilder.appendTo(popup, "Selection");
	}

	private void buildFromSelectionToOther(ModelComponent livePanel, JPopupMenu popup, final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionSelectionGeneralMapBuilder.addTransaction("Unforward to", new Runnable() {
				@Override
				public void run() {
					selection.getTransactionFactory().executeOnRoot(
						new Model.RemoveObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
					);
				}
			});
		} else {
			transactionSelectionGeneralMapBuilder.addTransaction("Forward to", new Runnable() {
				@Override
				public void run() {
					selection.getTransactionFactory().executeOnRoot(
						new Model.AddObserver(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
					);
				}
			});
		}
		
		// Only available for canvases:
		if(target.getModelBehind() instanceof CanvasModel) {
			TransactionMapBuilder transactionObserverMapBuilder = new TransactionMapBuilder();
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
			transactionObserverMapBuilder.addTransaction("Cons", transactionObserverContentMapBuilder);
			transactionObserverMapBuilder.appendTo(popup, "Observation");
		}
		
		transactionSelectionGeneralMapBuilder.addTransaction("Inject", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
					new InjectTransaction(selection.getTransactionFactory().getLocation(), target.getTransactionFactory().getLocation())
				);
			}
		});

		TransactionMapBuilder transactionTargetMapBuilder = new TransactionMapBuilder();
		target.appendDropTargetTransactions(selection, dropBoundsOnTarget, dropPointOnTarget, transactionTargetMapBuilder);
		
		transactionSelectionGeneralMapBuilder.appendTo(popup, "General");
		if(!transactionSelectionGeneralMapBuilder.isEmpty() && !transactionTargetMapBuilder.isEmpty())
			popup.addSeparator();
		transactionTargetMapBuilder.appendTo(popup, "Target");

		TransactionMapBuilder transactionDroppedMapBuilder = new TransactionMapBuilder();
		selection.appendDroppedTransactions(livePanel, target, dropBoundsOnTarget, transactionDroppedMapBuilder);
		if(!transactionTargetMapBuilder.isEmpty() && !transactionDroppedMapBuilder.isEmpty())
			popup.addSeparator();
		transactionDroppedMapBuilder.appendTo(popup, "Dropped");
	}
}
