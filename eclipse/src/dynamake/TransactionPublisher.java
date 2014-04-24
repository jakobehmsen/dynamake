package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

public interface TransactionPublisher {
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child);
	void appendTransactions(TransactionMapBuilder transactions);
	void appendDroppedTransactions(TransactionMapBuilder transactions);
	void appendDropTargetTransactions(ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions);
}
