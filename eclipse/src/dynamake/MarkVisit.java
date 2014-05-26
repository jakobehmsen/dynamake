package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

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
	public Model modelCloneIsolated() {
		return new MarkVisit(model);
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceConnection<Model> connection) {
		propCtx.markVisitedBy(model);
		// calling super.changed should not invoked this way
		// How to call it instead, such as for changing properties?
		// Perhaps, a meta observer should be possible to extract?
//		super.changed(this, change, newPropCtx);
		sendChanged(change, propCtx, propDistance, changeDistance, connection);
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
		public Model getModelBehind() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child, PrevaylerServiceConnection<Model> connection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void initialize() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
		}
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView,
			ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
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
