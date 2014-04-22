package dynamake;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import org.prevayler.Transaction;

/**
 * Assumed only to be implemented by JComponent (or JFrame) class extensions.
 */
public interface ModelComponent {
	Model getModel();
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
	void appendTransactions(TransactionMapBuilder transactions);
	TransactionFactory getTransactionFactory();
	Transaction<Model> getDefaultDropTransaction(ModelComponent dropped, Point dropPoint);
	void appendDroppedTransactions(TransactionMapBuilder transactions);
	void appendDropTargetTransactions(ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions);
	
	public static class Util {
		public static ModelComponent getParent(ModelComponent view) {
			Component parent = ((Component)view).getParent();
			while(parent != null && !(parent instanceof ModelComponent))
				parent = parent.getParent();
			return (ModelComponent)parent;
		}
	}
}
