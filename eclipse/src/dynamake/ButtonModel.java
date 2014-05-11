package dynamake;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JButton;

import org.prevayler.Transaction;

/*

Requires new drag/drop option for texts: when dropping a text on a canvas: "For new button"?
How to change the text of a button onwards? Drop texts on it with a "Set text" option?

Perhaps there should be a "text" mode/tool for general text editing of the model supporting it?
- This could then work as a short-hand manner to change the text of a button
  - Should this then also be used for changing the text of text models? 
    - If so, then, what is the use of the "use" mode/tool? 
      - Perhaps, such a mode doesn't sense; everything action is naturally about production?

If the number of changes send out from text models are extended to include such changes as mouse down, mouse up, 
then perhaps it is possible to simulate buttons within the environment itself? 

*/
public class ButtonModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String text;

	public ButtonModel(String text) {
		this.text = text;
	}
	
	@Override
	public Model modelCloneIsolated() {
		return new ButtonModel(text);
	}

	private static class ButtonView extends JButton implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ButtonModel model;
		private TransactionFactory transactionFactory;
		
		public ButtonView(ButtonModel model,
				TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
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
		public void appendDropTargetTransactions(ModelComponent dropped,
				Rectangle droppedBounds, Point dropPoint,
				TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			final TransactionFactory transactionFactory) {
		final ButtonView view = new ButtonView(this, transactionFactory);
		
		view.setText(text);
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		final Model.RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view, view, viewManager);
		
		Model.loadComponentProperties(this, view);
		
		viewManager.wasCreated(view);

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removeListenerForBoundChanges.releaseBinding();
				removeListenerForComponentPropertyChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
