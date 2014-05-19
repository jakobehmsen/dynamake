package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class DragDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(Runner runner,
			ModelComponent livePanel, JPopupMenu popup,
			ModelComponent selection, ModelComponent target, Point dropPointOnTarget, Rectangle dropBoundsOnTarget) {
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

	private void buildFromSelectionToOther(final ModelComponent livePanel, JPopupMenu popup, final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionSelectionGeneralMapBuilder.addTransaction("Unforward to", new Runnable() {
				@Override
				public void run() {
					selection.getTransactionFactory().executeOnRoot(
						new PropogationContext(), new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
					);
				}
			});
		} else {
			transactionSelectionGeneralMapBuilder.addTransaction("Forward to", new Runnable() {
				@Override
				public void run() {
					selection.getTransactionFactory().executeOnRoot(
						new PropogationContext(), new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
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
			transactionObserverMapBuilder.addTransaction("Cons", transactionObserverContentMapBuilder);
			transactionObserverMapBuilder.appendTo(popup, "Observation");
		}
		
		transactionSelectionGeneralMapBuilder.addTransaction("Inject", new Runnable() {
			@Override
			public void run() {
				selection.getTransactionFactory().executeOnRoot(
					new PropogationContext(), new InjectTransaction(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
				);
			}
		});

		TransactionMapBuilder transactionTargetMapBuilder = new TransactionMapBuilder();
		target.appendDropTargetTransactions(livePanel, selection, dropBoundsOnTarget, dropPointOnTarget, transactionTargetMapBuilder);
		
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
