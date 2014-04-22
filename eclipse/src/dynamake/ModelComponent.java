package dynamake;

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
}
