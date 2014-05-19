package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class NotVisited extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Model model;
	
	public NotVisited(Model model) {
		this.model = model;
	}
	
	@Override
	public Model modelCloneIsolated() {
		return new NotVisited(model);
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		if(!propCtx.isMarkedVisitedBy(model))
			sendChanged(change, propCtx, propDistance, changeDistance);
	}
	
	private static class NotVisitedView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private NotVisited model;
		private TransactionFactory transactionFactory;

		public NotVisitedView(NotVisited model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel("Not Visited", JLabel.CENTER), BorderLayout.CENTER);
		}

		@Override
		public Model getModelBehind() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Command<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView,
			ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final NotVisitedView view = new NotVisitedView(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
