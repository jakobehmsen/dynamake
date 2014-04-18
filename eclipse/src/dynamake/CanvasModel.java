package dynamake;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import org.prevayler.Transaction;

public class CanvasModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Model> models = new ArrayList<Model>();
	
	public static class AddedModelChange {
		public final int index;
		public final Model model;
		
		public AddedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	public static class RemovedModelChange {
		public final int index;
		public final Model model;
		
		public RemovedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	public static class AddModelTransaction implements Transaction<CanvasModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Rectangle creationBounds;
		private Factory factory;
		
		public AddModelTransaction(Rectangle creationBounds, Factory factory) {
			this.creationBounds = creationBounds;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(CanvasModel prevalentSystem, Date executionTime) {
			Model model = (Model)factory.create(new Hashtable<String, Object>());

			model.setProperty("X", creationBounds.x);
			model.setProperty("Y", creationBounds.y);
			model.setProperty("Width", creationBounds.width);
			model.setProperty("Height", creationBounds.height);
			
			prevalentSystem.addModel(model);
		}
	}
	
	public static class RemoveModelTransaction implements Transaction<CanvasModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public RemoveModelTransaction(int index) {
			this.index = index;
		}
		
		@Override
		public void executeOn(CanvasModel prevalentSystem, Date executionTime) {
			prevalentSystem.removeModel(index);
		}
	}
	
	public void addModel(Model model) {
		addModel(models.size(), model);
	}
	
	public void addModel(int index, Model model) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model));
	}
	
	public void removeModel(Model model) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel);
	}
	
	public void removeModel(int index) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model));
	}
	
	public int indexOfModel(Model model) {
		return models.indexOf(model);
	}
	
	private static class CanvasPanel extends JLayeredPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CanvasModel model;
		private TransactionFactory transactionFactory;
		
		public CanvasPanel(CanvasModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			setLayout(null);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setOpaque(true);
			
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == 3)
						viewManager.selectAndActive(CanvasPanel.this, e.getX(), e.getY());
				}
			});
		}

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, final ModelComponent child) {
			transactions.addTransaction("Remove", new Runnable() {
				@Override
				public void run() {
					int indexOfModel = model.indexOfModel(child.getModel());
					transactionFactory.execute(new RemoveModelTransaction(indexOfModel));
				}
			});
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
	}
	
	private static class IndexLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public IndexLocation(int index) {
			this.index = index;
		}

		@Override
		public Object getChild(Object holder) {
			return ((CanvasModel)holder).models.get(index);
		}

		@Override
		public void setChild(Object holder, Object child) {
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final CanvasPanel view = new CanvasPanel(this, transactionFactory, viewManager);
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view);
		Model.loadComponentProperties(this, view);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view);
		
		for(final Model model: models) {
			Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new Locator() {
				@Override
				public Location locate() {
					int index = indexOfModel(model);
					return new IndexLocation(index);
				}
			}));
			
			Rectangle bounds = new Rectangle(
				(int)model.getProperty("X"),
				(int)model.getProperty("Y"),
				(int)model.getProperty("Width"),
				(int)model.getProperty("Height")
			);
			
			((JComponent)modelView.getBindingTarget()).setBounds(bounds);
			
			view.add((JComponent)modelView.getBindingTarget());
			view.setLayer((JComponent)modelView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof CanvasModel.AddedModelChange) {
					final Model model = ((CanvasModel.AddedModelChange)change).model;
					
					Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new Locator() {
						@Override
						public Location locate() {
							int index = indexOfModel(model);
							return new IndexLocation(index);
						}
					}));
					
					Rectangle bounds = new Rectangle(
						(int)model.getProperty("X"),
						(int)model.getProperty("Y"),
						(int)model.getProperty("Width"),
						(int)model.getProperty("Height")
					);
					
					((JComponent)modelView.getBindingTarget()).setBounds(bounds);
					
					view.add((JComponent)modelView.getBindingTarget());
					view.setLayer((JComponent)modelView.getBindingTarget(), JLayeredPane.DEFAULT_LAYER);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					view.remove(((CanvasModel.RemovedModelChange)change).index);
					view.validate();
					view.repaint();
					viewManager.clearFocus();
				}
			}
		});

		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				removableListenerForComponentPropertyChanges.releaseBinding();
				removableListenerForBoundsChanges.releaseBinding();
				removableListener.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
