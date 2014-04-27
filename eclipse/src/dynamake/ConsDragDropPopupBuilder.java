package dynamake;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import org.prevayler.Transaction;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionToSelection(JPopupMenu popup,
			ModelComponent selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void buildFromSelectionToOther(JPopupMenu popup,
			final ModelComponent selection, final ModelComponent target,
			final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
//		TransactionMapBuilder transactionObserverMapBuilder = new TransactionMapBuilder();
		
		Transaction<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			selection.getTransactionFactory().executeOnRoot(implicitDropAction);
		} else {
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModel().isObservedBy(target.getModel())) {
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
//						Dimension size = new Dimension(120, 50);
//						Rectangle bounds = new Rectangle(dropPointOnTarget, size);
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
	
			TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
			
//			transactionSelectionGeneralMapBuilder.addTransaction("Mark Visit",
//				new Runnable() {
//					@Override
//					public void run() {
//						// HACK: Models can only be added to canvases
//						if(target.getModel() instanceof CanvasModel) {
//							Dimension size = new Dimension(120, 50);
//							Rectangle bounds = new Rectangle(dropPointOnTarget, size);
//							Factory itemFactory = new CreationModelFactory(new CreateAndBindFactory(new MarkVisitByFactory(), selection.getTransactionFactory().getLocation()), new String[]{"By"});
//							target.getTransactionFactory().executeOnRoot(
//								new CanvasModel.AddModelTransaction(
//									target.getTransactionFactory().getLocation(), bounds, itemFactory));
//						}
//					}
//				}
//			);
//			
//			transactionSelectionGeneralMapBuilder.addTransaction("Not Visited",
//				new Runnable() {
//					@Override
//					public void run() {
//						// Find the selected model and attempt an add model transaction
//						// HACK: Models can only be added to canvases
//						if(target.getModel() instanceof CanvasModel) {
////							Dimension size = new Dimension(120, 50);
////							Rectangle bounds = new Rectangle(dropPointOnTarget, size);
////							target.getTransactionFactory().executeOnRoot(
////								new CanvasModel.AddModelTransaction(
////									target.getTransactionFactory().getLocation(), bounds, new NotVisitedFactory(selection.getTransactionFactory().getLocation())));
//							
//
//							Dimension size = new Dimension(120, 50);
//							Rectangle bounds = new Rectangle(dropPointOnTarget, size);
//							Factory itemFactory = new CreationModelFactory(new CreateAndBindFactory(new NotVisitedByFactory(), selection.getTransactionFactory().getLocation()), new String[]{"By"});
//							target.getTransactionFactory().executeOnRoot(
//								new CanvasModel.AddModelTransaction(
//									target.getTransactionFactory().getLocation(), bounds, itemFactory));
//						}
//					}
//				}
//			);
			
			transactionSelectionGeneralMapBuilder.appendTo(popup, "General");
		}
	}
}
