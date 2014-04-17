package dynamake;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * Assumed only to be implemented by JComponent (or Frame) class extensions.
 */
public interface ModelComponent {
//	ModelBroker getModel();
	Model getModel();
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
	void appendTransactions(TransactionMapBuilder transactions);
	Color getPrimaryColor();
	TransactionFactory getTransactionFactory();
}
