package dynamake;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * Assumed only to be implemented by JComponent classes.
 */
public interface ModelComponent {
//	ModelBroker getModel();
	Model getModel();
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
	void appendTransactions(TransactionMapBuilder transactions);
	Color getPrimaryColor();
	void create(Factory factory, Rectangle creationBounds);
	TransactionFactory getTransactionFactory();
}