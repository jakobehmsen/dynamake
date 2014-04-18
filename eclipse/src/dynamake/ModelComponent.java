package dynamake;

import java.awt.Point;

import org.prevayler.Transaction;

/**
 * Assumed only to be implemented by JComponent (or JFrame) class extensions.
 */
public interface ModelComponent {
	Model getModel();
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
	void appendTransactions(TransactionMapBuilder transactions);
	TransactionFactory getTransactionFactory();
	Transaction<? extends Model> getDefaultDropTransaction(Point dropPoint);
}
