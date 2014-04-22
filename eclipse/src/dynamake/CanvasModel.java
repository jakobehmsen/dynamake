package dynamake;

import java.awt.Color;
import java.awt.Point;
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
	
	public static class MoveModelTransaction implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private Location modelLocation;
		private Point point;
		
		public MoveModelTransaction(Location canvasSourceLocation, Location canvasTargetLocation, Location modelLocation, Point point) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.modelLocation = modelLocation;
			this.point = point;
		}
		
		@Override
		public void executeOn(Model rootPrevalentSystem, Date executionTime) {
			PropogationContext propCtx = new PropogationContext();
			
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(rootPrevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(rootPrevalentSystem);
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);

			canvasSource.removeModel(model, propCtx);
			model.getMetaModel().beginUpdate(propCtx);
			model.getMetaModel().set("X", point.x, propCtx);
			model.getMetaModel().set("Y", point.y, propCtx);
			model.getMetaModel().endUpdate(propCtx);
			canvasTarget.addModel(model, propCtx);
		}
	}
	
	public static class AddModelTransaction implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		private Factory factory;
		
		public AddModelTransaction(Location canvasLocation, Rectangle creationBounds, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(Model rootPrevalentSystem, Date executionTime) {
			PropogationContext propCtx = new PropogationContext();
			
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, new Hashtable<String, Object>());

			model.getMetaModel().set("X", creationBounds.x, propCtx);
			model.getMetaModel().set("Y", creationBounds.y, propCtx);
			model.getMetaModel().set("Width", creationBounds.width, propCtx);
			model.getMetaModel().set("Height", creationBounds.height, propCtx);
			
			canvas.addModel(model, new PropogationContext());
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
			prevalentSystem.removeModel(index, new PropogationContext());
		}
	}
	
	public void addModel(Model model, PropogationContext propCtx) {
		addModel(models.size(), model, propCtx);
	}
	
	public void addModel(int index, Model model, PropogationContext propCtx) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model), propCtx);
	}
	
	public void removeModel(Model model, PropogationContext propCtx) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx);
	}
	
	public void removeModel(int index, PropogationContext propCtx) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model), propCtx);
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
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent dropped,
				final Rectangle droppedBounds, final Point dropPoint, TransactionMapBuilder transactions) {
			if(dropped.getTransactionFactory().getParent() != null) {
				transactions.addTransaction("Move", new Runnable() {
					@Override
					public void run() {
						Location canvasSourceLocation = dropped.getTransactionFactory().getParent().getLocation();
						Location canvasTargetLocation = transactionFactory.getLocation();
						Location modelLocation = dropped.getTransactionFactory().getLocation();
						
						transactionFactory.executeOnRoot(new MoveModelTransaction(canvasSourceLocation, canvasTargetLocation, modelLocation, droppedBounds.getLocation()));
//						int indexOfModel = model.indexOfModel(child.getModel());
//						transactionFactory.execute(new RemoveModelTransaction(indexOfModel));
					}
				});
			}
		}
	}
	
	private static class IndexLocator implements Locator {
		private CanvasModel canvasModel;
		private Model model;

		public IndexLocator(CanvasModel canvasModel, Model model) {
			this.canvasModel = canvasModel;
			this.model = model;
		}

		@Override
		public Location locate() {
			int index = canvasModel.indexOfModel(model);
			return new IndexLocation(index);
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
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		Model.loadComponentProperties(this, view);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view, view, viewManager);
		
		for(final Model model: models) {
			Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new IndexLocator(this, model)));
			
			Rectangle bounds = new Rectangle(
				(int)model.getMetaModel().get("X"),
				(int)model.getMetaModel().get("Y"),
				(int)model.getMetaModel().get("Width"),
				(int)model.getMetaModel().get("Height")
			);
			
			((JComponent)modelView.getBindingTarget()).setBounds(bounds);
			
			view.add((JComponent)modelView.getBindingTarget());
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx) {
				if(change instanceof CanvasModel.AddedModelChange) {
					final Model model = ((CanvasModel.AddedModelChange)change).model;
					
					Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new IndexLocator(CanvasModel.this, model)));
					
					Rectangle bounds = new Rectangle(
						(int)model.getMetaModel().get("X"),
						(int)model.getMetaModel().get("Y"),
						(int)model.getMetaModel().get("Width"),
						(int)model.getMetaModel().get("Height")
					);
					
					((JComponent)modelView.getBindingTarget()).setBounds(bounds);
					
					view.add((JComponent)modelView.getBindingTarget());
					viewManager.refresh(view);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					view.remove(((CanvasModel.RemovedModelChange)change).index);
					view.validate();
					view.repaint();
					viewManager.clearFocus();
					viewManager.repaint(view);
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
