package dynamake;

import java.awt.Component;

/**
 * Assumed only to be implemented by JComponent (or JFrame) class extensions.
 */
public interface ModelComponent {
	Model getModel();
	TransactionFactory getTransactionFactory();
//	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
//	void appendTransactions(TransactionMapBuilder transactions);
//	void appendDroppedTransactions(TransactionMapBuilder transactions);
//	void appendDropTargetTransactions(ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions);
	TransactionPublisher getObjectTransactionPublisher();
//	TransactionPublisher getMetaTransactionPublisher();
//	TransactionPublisher getConsTransactionPublisher();
	
	public static class Util {
		public static ModelComponent getParent(ModelComponent view) {
			Component parent = ((Component)view).getParent();
			while(parent != null && !(parent instanceof ModelComponent))
				parent = parent.getParent();
			return (ModelComponent)parent;
		}
	}
}
