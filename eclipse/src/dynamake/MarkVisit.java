package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class MarkVisit extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Model model;
	
	public MarkVisit(Model model) {
		this.model = model;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx) {
		PropogationContext newPropCtx = propCtx.markVisitedBy(model);
//		super.changed(this, change, newPropCtx);
		sendChanged(change, newPropCtx);
	}
	
	private static class MarkVisitedByView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private MarkVisit model;
		private TransactionFactory transactionFactory;

		public MarkVisitedByView(MarkVisit model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel("Mark Visit", JLabel.CENTER), BorderLayout.CENTER);
		}

		@Override
		public Model getModel() {
			return model;
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
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public Transaction<Model> getDefaultDropTransaction(
				ModelComponent dropped, Point dropPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void appendDroppedTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final MarkVisitedByView view = new MarkVisitedByView(this, transactionFactory);
		
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
