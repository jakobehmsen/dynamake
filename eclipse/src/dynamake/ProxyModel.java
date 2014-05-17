package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class ProxyModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model model;
	
	public ProxyModel(Model model) {
		this.model = model;
	}
	
	@Override
	public Model modelCloneIsolated() {
		return new ProxyModel(model);
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		model.changed(sender, change, propCtx, propDistance, changeDistance);
	}
	
	@Override
	public void addObserver(Observer observer) {
		model.addObserver(observer);
	}
	
//	@Override
//	public void removeObserver(Observer observer) {
//		model.removeObserver(observer);
//	}
//	
//	@Override
//	public void beginUpdate(PropogationContext propCtx) {
//		model.beginUpdate(propCtx);
//	}
//	
//	@Override
//	public void endUpdate(PropogationContext propCtx) {
//		model.endUpdate(propCtx);
//	}
	
	@Override
	public boolean isObservedBy(Observer observer) {
		return model.isObservedBy(observer);
	}
	
	private static class ProxyModelView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ProxyModel model;
		private TransactionFactory transactionFactory;
		private ModelComponent view;

		public ProxyModelView(ProxyModel model, TransactionFactory transactionFactory, ModelComponent modelView) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			this.view = modelView;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel("Proxy", JLabel.CENTER), BorderLayout.CENTER);
		}

		@Override
		public Model getModelBehind() {
//			return view.getModel();
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
//			return view.getTransactionFactory();
			return transactionFactory;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
//			view.appendContainerTransactions(transactions, child);
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
//			view.appendTransactions(transactions);
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
//			view.appendDropTargetTransactions(dropped, droppedBounds, dropPoint, transactions);
		}

		@Override
		public Command<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	private static class ModelLocator implements Locator {
		@Override
		public Location locate() {
			return new ModelLocation();
		}
	}
	
	private static class ModelLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object getChild(Object holder) {
			return ((ProxyModel)holder).model;
		}
		
		@Override
		public void setChild(Object holder, Object child) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new ModelLocator()));
		
		final ProxyModelView view = new ProxyModelView(this, transactionFactory, modelView.getBindingTarget());
		
		final Binding<Model> removableListener = RemovableListener.addAll(this, 
			bindProperty(this, "Background", new Action1<Color>() {
				public void run(Color value) {
					view.setBackground(value);
					viewManager.refresh(view);
				}
			}),
			bindProperty(this, "Foreground", new Action1<Color>() {
				public void run(Color value) {
					view.setForeground(value);
					viewManager.refresh(view);
				}
			})
		);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		viewManager.wasCreated(view);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListener.releaseBinding();
				removableListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
