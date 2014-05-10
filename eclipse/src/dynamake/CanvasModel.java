package dynamake;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import org.prevayler.Transaction;

public class CanvasModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Model> models;
	
	public CanvasModel() {
		models = new ArrayList<Model>();
	}
	
	public CanvasModel(ArrayList<Model> models) {
		this.models = models;
	}
	
	public static class AddedModelChange {
		public final int index;
		public final Model model;
		
		public AddedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	@Override
	public Model modelCloneIsolated() {
		ArrayList<Model> clonedModels = new ArrayList<Model>();
		
		for(Model model: models) {
			Model clone = model.cloneIsolated();
			clonedModels.add(clone);
		}
		
		return new CanvasModel(clonedModels);
	}
	
	@Override
	protected void modelAddContent(HashSet<Model> contained) {
		for(Model model: models) {
			model.addContent(contained);
		}
	}

	@Override
	protected void cloneAndMap(Hashtable<Model, Model> sourceToCloneMap) {
		CanvasModel clone = new CanvasModel();
		clone.properties = new Hashtable<String, Object>();
		// Assumed that cloning is not necessary for properties
		// I.e., all property values are immutable
		clone.properties.putAll(this.properties);
		sourceToCloneMap.put(this, clone);
		
		for(Model model: models) {
			model.cloneAndMap(sourceToCloneMap);
			Model modelClone = sourceToCloneMap.get(model);
			clone.models.add(modelClone);
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

			canvasSource.removeModel(model, propCtx, 0);
			model.beginUpdate(propCtx, 0);
			model.setProperty("X", point.x, propCtx, 0);
			model.setProperty("Y", point.y, propCtx, 0);
			model.endUpdate(propCtx, 0);
			canvasTarget.addModel(model, propCtx, 0);
		}
	}
	
	public static class AddModelTransaction implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		Hashtable<String, Object> creationArgs;
		private Factory factory;
		
		public AddModelTransaction(Location canvasLocation, Rectangle creationBounds, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
			this.creationArgs = new Hashtable<String, Object>();
		}
		
		public AddModelTransaction(Location canvasLocation, Rectangle creationBounds, Hashtable<String, Object> creationArgs, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.creationArgs = creationArgs;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(Model rootPrevalentSystem, Date executionTime) {
			PropogationContext propCtx = new PropogationContext();
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationArgs);

			model.setProperty("X", creationBounds.x, propCtx, 0);
			model.setProperty("Y", creationBounds.y, propCtx, 0);
			model.setProperty("Width", creationBounds.width, propCtx, 0);
			model.setProperty("Height", creationBounds.height, propCtx, 0);
			
			canvas.addModel(model, new PropogationContext(), 0);
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
			Model modelToRemove = prevalentSystem.getModel(index);
			prevalentSystem.removeModel(index, new PropogationContext(), 0);
			modelToRemove.beRemoved();
		}
	}
	
	public void addModel(Model model, PropogationContext propCtx, int propDistance) {
		addModel(models.size(), model, propCtx, propDistance);
	}
	
	public Model getModel(int index) {
		return models.get(index);
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0);
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
		public Model getModelBehind() {
			return model;
		}
		
		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, final ModelComponent child) {
			transactions.addTransaction("Remove", new Runnable() {
				@Override
				public void run() {
					int indexOfModel = model.indexOfModel(child.getModelBehind());
					transactionFactory.execute(new RemoveModelTransaction(indexOfModel));
				}
			});
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
		}
		@Override
		public void appendDroppedTransactions(ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(this, target, droppedBounds, transactions);
		}
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent dropped,
				final Rectangle droppedBounds, final Point dropPoint, TransactionMapBuilder transactions) {
			if(dropped.getTransactionFactory().getParent() != null && 
				dropped.getTransactionFactory().getParent() != CanvasPanel.this.transactionFactory &&
				!isContainerOf(dropped.getTransactionFactory(), this.transactionFactory) /*Dropee cannot be child of dropped*/) {
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
		
		private boolean isContainerOf(TransactionFactory container, TransactionFactory item) {
			TransactionFactory parent = item.getParent();
			if(parent != null) {
				if(parent == container)
					return true;
				return isContainerOf(container, parent);
			}
			return false;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
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

	private void addModelComponent(
			final CanvasPanel view, 
			final TransactionFactory transactionFactory, final ViewManager viewManager, 
			final HashSet<Model> shownModels, 
			final Memoizer1<Model, Binding<ModelComponent>> modelToModelComponentMap,
			Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap,
			final Model model) {
		Integer viewModel2 = (Integer)model.getProperty(Model.PROPERTY_VIEW);
		if(viewModel2 == null)
			viewModel2 = 1;

		if(view.model.conformsToView(viewModel2)) {
			shownModels.add(model);
			
			Binding<ModelComponent> modelView = modelToModelComponentMap.call(model);

			view.add((JComponent)modelView.getBindingTarget());
		}
		
		Model.RemovableListener removableListener = Model.RemovableListener.addObserver(model, new Observer() {
			@Override
			public void removeObservee(Observer observee) { }
			
			@Override
			public void changed(Model sender, Object change,
					PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						int modelView2 = (int)propertyChanged.value;
						if(view.model.conformsToView(modelView2)) {
							// Should be shown
							if(!shownModels.contains(sender)) {
								Binding<ModelComponent> modelView = modelToModelComponentMap.call(model);
								
								shownModels.add(sender);
								view.add((JComponent)modelView.getBindingTarget());
								viewManager.refresh(view);
							}
						} else {
							// Should be hidden
							if(shownModels.contains(sender)) {
								Binding<ModelComponent> modelView = modelToModelComponentMap.call(model);
								
								shownModels.remove(sender);
								view.remove((JComponent)modelView.getBindingTarget());
								viewManager.unFocus(modelView.getBindingTarget());
								viewManager.refresh(view);
							}
						}
					}
				}
			}
			
			@Override
			public void addObservee(Observer observee) { }
		});
		
		modelToRemovableListenerMap.put(model, removableListener);
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final CanvasPanel view = new CanvasPanel(this, transactionFactory, viewManager);
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		Model.loadComponentProperties(this, view);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view, view, viewManager);
		Model.wrapForComponentGUIEvents(this, view, view, viewManager);
		
		final HashSet<Model> shownModels = new HashSet<Model>();
		final Memoizer1<Model, Binding<ModelComponent>> modelToModelComponentMap = new Memoizer1<Model, Binding<ModelComponent>>(new Func1<Model, Binding<ModelComponent>>() {
			@Override
			public Binding<ModelComponent> call(Model model) {
				final Binding<ModelComponent> modelView = model.createView(viewManager, transactionFactory.extend(new IndexLocator(CanvasModel.this, model)));
				
				Rectangle bounds = new Rectangle(
					(int)model.getProperty("X"),
					(int)model.getProperty("Y"),
					(int)model.getProperty("Width"),
					(int)model.getProperty("Height")
				);
				
				((JComponent)modelView.getBindingTarget()).setBounds(bounds);
				
//				System.out.println("Created view");
				
				return modelView;
			}
		});
		
		final Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap = new Hashtable<Model, Model.RemovableListener>();
		for(final Model model: models) {
//		// Reverse order such that the last added model is put to the front
//		for(int i = models.size() - 1; i >= 0; i--) {
			addModelComponent(
				view, transactionFactory, viewManager, shownModels, modelToModelComponentMap, modelToRemovableListenerMap, model
			);
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof CanvasModel.AddedModelChange) {
					final Model model = ((CanvasModel.AddedModelChange)change).model;
					
					addModelComponent(
						view, transactionFactory, viewManager, shownModels, modelToModelComponentMap, modelToRemovableListenerMap, model
					);
					viewManager.refresh(view);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					// It could be possible to have map mapping from model to model component as follows:
					Model removedModel = ((CanvasModel.RemovedModelChange)change).model;
					
					Binding<ModelComponent> removedMCBinding = modelToModelComponentMap.get(removedModel);
					modelToModelComponentMap.clear(removedModel);
					ModelComponent removedMC = removedMCBinding.getBindingTarget();
					view.remove((JComponent)removedMC);
					
					Model.RemovableListener removableListener = modelToRemovableListenerMap.get(removedModel);
					removableListener.releaseBinding();
					
//					int componentIndex = (view.getComponentCount() - 1) - ((CanvasModel.RemovedModelChange)change).index;
//					view.remove(componentIndex);
//					view.remove(((CanvasModel.RemovedModelChange)change).index);
					view.validate();
					view.repaint();
					viewManager.clearFocus();
					viewManager.repaint(view);
				} else if(change instanceof Model.PropertyChanged && propDistance == 1) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						ArrayList<Model> invisibles = new ArrayList<Model>();
						for(Model m: view.model.models) {
							boolean wasFound = false;
							for(Component mc: view.getComponents()) {
								if(m == ((ModelComponent)mc).getModelBehind()) {
									wasFound = true;
									break;
								}
							}
							if(!wasFound)
								invisibles.add(m);
						}
						
						Hashtable<Integer, Model> newVisibles = new Hashtable<Integer, Model>();

						for(int i = 0; i < invisibles.size(); i++) {
							Model invisible = invisibles.get(i);
							if(invisible.conformsToView((int)propertyChanged.value)) {
								newVisibles.put(i, invisible);
							}
						}
						
						ArrayList<Component> newInvisibles = new ArrayList<Component>();
						for(Component mc: view.getComponents()) {
							if(!((ModelComponent)mc).getModelBehind().viewConformsTo((int)propertyChanged.value)) {
								newInvisibles.add(mc);
							}
						}
						
						for(Component newInvisible: newInvisibles) {
							shownModels.remove(((ModelComponent)newInvisible).getModelBehind());
							view.remove(newInvisible);
						}
						
						Object[] visibles = new Object[view.model.models.size()];
						for(Component mc: view.getComponents()) {
							int indexOfVisible = view.model.indexOfModel(((ModelComponent)mc).getModelBehind());
							visibles[indexOfVisible] = mc;
						}
						
						// Add the new visibles at each their respective index at model into visibles
						for(Map.Entry<Integer, Model> entry: newVisibles.entrySet()) {
							visibles[entry.getKey()] = entry.getValue();
						}
						
//						int zOrder = view.getComponentCount() + newVisibles.size() - 1;
						for(int i = 0; i < visibles.length; i++) {
							Object visible = visibles[i];
							if(visible != null) {
								if(visible instanceof Model) {
									// Model to add
									Model model = (Model)visible;
									shownModels.add(model);
									Binding<ModelComponent> modelView = modelToModelComponentMap.call(model);

									view.add((JComponent)modelView.getBindingTarget());
									
//									// Reverse the index starting from the last index at zero, second last as 1, and so forth
//									// Put the last added model to the front
//									view.setComponentZOrder((JComponent)modelView.getBindingTarget(), zOrder);
								}
//								zOrder--;
							}
						}
						viewManager.refresh(view);
					}
				}
			}
		});
		
		viewManager.wasCreated(view);

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
