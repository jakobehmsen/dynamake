package dynamake;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import org.prevayler.Transaction;

/**
 * Assumed only to be implemented by JComponent (or JFrame) class extensions.
 */
public interface ModelComponent {
	Model getModelBehind();
	TransactionFactory getTransactionFactory();
	void appendContainerTransactions(TransactionMapBuilder transactions, ModelComponent child, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch);
	void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch);
	void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch);
	void appendDropTargetTransactions(ModelComponent livePanel, ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch);
	void initialize();
	
	public static class Util {
		public static ModelComponent getParent(ModelComponent view) {
			Component parent = ((Component)view).getParent();
			while(parent != null && !(parent instanceof ModelComponent))
				parent = parent.getParent();
			return (ModelComponent)parent;
		}
		
		public static ModelComponent closestModelComponent(Component component) {
			while(component != null && !(component instanceof ModelComponent))
				component = component.getParent();
			return (ModelComponent)component;
		}
	}

	DualCommandFactory<Model> getImplicitDropAction(ModelComponent target);
	void visitTree(Action1<ModelComponent> visitAction);
}
