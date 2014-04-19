package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class BackgroundSetter extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public void changed(Model sender, Object change) {
		if(change instanceof Model.Atom) {
			Model.Atom atom = (Model.Atom)change;
			Model.SetProperty setProperty = new SetProperty("Background", atom.value);
			sendChanged(setProperty);
		} else {
			super.changed(sender, change);
		}
	}
	
	private static class BackgroundSetterView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private BackgroundSetter model;
		private TransactionFactory transactionFactory;

		public BackgroundSetterView(BackgroundSetter model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel("BG Setter", JLabel.CENTER), BorderLayout.CENTER);
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
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final BackgroundSetterView view = new BackgroundSetterView(this, transactionFactory);
		
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
