package dynamake;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class DragDragDropPopupBuilder implements DragDropPopupBuilder {

	@Override
	public void buildFromSelectionToSelection(JPopupMenu popup, ModelComponent selection) {
		ModelComponent parentModelComponent = ModelComponent.Util.closestModelComponent(((JComponent)selection).getParent()); 
		
		TransactionPublisher containerTransactionPublisher = parentModelComponent.getObjectTransactionPublisher();
		TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
		if(parentModelComponent != null)
			containerTransactionPublisher.appendContainerTransactions(containerTransactionMapBuilder, selection);

		TransactionPublisher selectionTransactionPublisher = selection.getObjectTransactionPublisher();
		TransactionMapBuilder transactionSelectionMapBuilder = new TransactionMapBuilder();
		selectionTransactionPublisher.appendTransactions(transactionSelectionMapBuilder);

		containerTransactionMapBuilder.appendTo(popup, "Container");
		if(!containerTransactionMapBuilder.isEmpty() && !transactionSelectionMapBuilder.isEmpty())
			popup.addSeparator();
		transactionSelectionMapBuilder.appendTo(popup, "Selection");
	}

	@Override
	public void buildFromSelectionToOther(JPopupMenu popup, final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
//		final Point pointOnTargetOver = SwingUtilities.convertPoint(popupMenuInvoker, pointOnInvoker, (JComponent)targetOver);
//		
//		final Rectangle droppedBounds = SwingUtilities.convertRectangle(ProductionPanel.this, selectionFrame.getBounds(), (JComponent)targetOver);
		
		if(target.getModel().isObservedBy(selection.getModel())) {
			transactionSelectionGeneralMapBuilder.addTransaction("Unbind", 
				new Runnable() {
					@Override
					public void run() {
						target.getTransactionFactory().executeOnRoot(
							new Model.RemoveObserver(target.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation())
						);
					}
				}
			);
		} else {
			transactionSelectionGeneralMapBuilder.addTransaction("Bind", 
				new Runnable() {
					@Override
					public void run() {
						target.getTransactionFactory().executeOnRoot(
							new Model.AddObserver(target.getTransactionFactory().getLocation(), selection.getTransactionFactory().getLocation())
						);
					}
				}
			);
		}
		
		transactionSelectionGeneralMapBuilder.addTransaction("Mark Visit",
			new Runnable() {
				@Override
				public void run() {
					// Find the selected model and attempt an add model transaction
					// HACK: Models can only be added to canvases
					if(target.getModel() instanceof CanvasModel) {
						Dimension size = new Dimension(80, 50);
						Rectangle bounds = new Rectangle(dropPointOnTarget, size);
						target.getTransactionFactory().executeOnRoot(
							new CanvasModel.AddModelTransaction(
								target.getTransactionFactory().getLocation(), bounds, new MarkVisitFactory(selection.getTransactionFactory().getLocation())));
					}
				}
			}
		);
		
		transactionSelectionGeneralMapBuilder.addTransaction("Not Visited",
			new Runnable() {
				@Override
				public void run() {
					// Find the selected model and attempt an add model transaction
					// HACK: Models can only be added to canvases
					if(target.getModel() instanceof CanvasModel) {
						Dimension size = new Dimension(80, 50);
						Rectangle bounds = new Rectangle(dropPointOnTarget, size);
						target.getTransactionFactory().executeOnRoot(
							new CanvasModel.AddModelTransaction(
								target.getTransactionFactory().getLocation(), bounds, new NotVisitedFactory(selection.getTransactionFactory().getLocation())));
					}
				}
			}
		);
		
		transactionSelectionGeneralMapBuilder.addTransaction("Meta Model",
			new Runnable() {
				@Override
				public void run() {
					// Find the selected model and attempt an add model transaction
					// HACK: Models can only be added to canvases
					if(target.getModel() instanceof CanvasModel) {
						Dimension size = new Dimension(80, 50);
						Rectangle bounds = new Rectangle(dropPointOnTarget, size);
						target.getTransactionFactory().executeOnRoot(
							new CanvasModel.AddModelTransaction(
								target.getTransactionFactory().getLocation(), bounds, new MetaModelFactory(selection.getTransactionFactory().getLocation())));
					}
				}
			}
		);
		
		// Only available for canvases:
		if(target.getModel() instanceof CanvasModel) {
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
			transactionObserverMapBuilder.addTransaction("Then", transactionObserverContentMapBuilder);
			transactionObserverMapBuilder.appendTo(popup, "Observation");
		}

		TransactionPublisher targetTransactionPublisher = target.getObjectTransactionPublisher();
		TransactionMapBuilder transactionTargetMapBuilder = new TransactionMapBuilder();
		targetTransactionPublisher.appendDropTargetTransactions(selection, dropBoundsOnTarget, dropPointOnTarget, transactionTargetMapBuilder);
		
		transactionSelectionGeneralMapBuilder.appendTo(popup, "General");
		if(!transactionSelectionGeneralMapBuilder.isEmpty() && !transactionTargetMapBuilder.isEmpty())
			popup.addSeparator();
		transactionTargetMapBuilder.appendTo(popup, "Target");

		TransactionPublisher selectionTransactionPublisher = selection.getObjectTransactionPublisher();
		TransactionMapBuilder transactionDroppedMapBuilder = new TransactionMapBuilder();
		selectionTransactionPublisher.appendDroppedTransactions(transactionDroppedMapBuilder);
		if(!transactionTargetMapBuilder.isEmpty() && !transactionDroppedMapBuilder.isEmpty())
			popup.addSeparator();
		transactionDroppedMapBuilder.appendTo(popup, "Dropped");
	}
}
