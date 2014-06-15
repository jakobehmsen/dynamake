package dynamake;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import dynamake.LiveModel.LivePanel;

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
	protected void modelAppendScale(Fraction hChange, Fraction vChange, List<DualCommand<Model>> dualCommands) {
		for(Model model: models) {
			model.appendScale(hChange, vChange, dualCommands);
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
	protected void modelBeRemoved() {
		for(Model model: models) {
			model.beRemoved();
		}
	}

	public int getModelCount() {
		return models.size();
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
	
	public static class MoveModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private int indexInSource;
		private int indexInTarget;

		public MoveModelTransaction(Location canvasSourceLocation, Location canvasTargetLocation, int indexInSource, int indexInTarget) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.indexInSource = indexInSource;
			this.indexInTarget = indexInTarget;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(prevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(prevalentSystem);
			Model model = (Model)canvasSource.getModel(indexInSource);
			
			PrevaylerServiceBranch<Model> removeBranch = branch.branch();
			PrevaylerServiceBranch<Model> addBranch = branch.branch();

			int indexOfModel = canvasSource.indexOfModel(model);
			canvasSource.removeModel(indexOfModel, propCtx, 0, removeBranch);
			
			canvasTarget.addModel(indexInTarget, model, propCtx, 0, addBranch);
			
			removeBranch.close();
			addBranch.close();
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
	
	public static class AddModelTransaction implements Command<Model> {
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, creationArgs, propCtx, 0, branch);

			PrevaylerServiceBranch<Model> setPropertyBranch = branch.isolatedBranch();
			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, setPropertyBranch);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, setPropertyBranch);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, setPropertyBranch);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, setPropertyBranch);
			
			canvas.addModel(model, new PropogationContext(), 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
	
	public static class AddModelNoCreationBoundsTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private int index;
		private Factory factory;
		
		public AddModelNoCreationBoundsTransaction(Location canvasLocation, int index, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.index = index;
			this.factory = factory;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, null, null, propCtx, 0, branch);
			canvas.addModel(index, model, new PropogationContext(), 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
	
	public static class RemoveModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private int index;
		
		public RemoveModelTransaction(Location canvasLocation, int index) {
			this.canvasLocation = canvasLocation;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModel(index);
			canvas.removeModel(index, propCtx, 0, branch);
			modelToRemove.beRemoved();
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
	
	public void addModel(Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		addModel(models.size(), model, propCtx, propDistance, branch);
	}

	public Model getModel(int index) {
		return models.get(index);
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, branch);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance, branch);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0, branch);
	}
	
	public static void move(CanvasModel canvasSource, CanvasModel canvasTarget, Model model, int indexInTarget, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		int indexOfModel = canvasSource.indexOfModel(model);
		canvasSource.models.remove(indexOfModel);
		canvasSource.sendChanged(new RemovedModelChange(indexOfModel, model), propCtx, propDistance, 0, null);
		canvasTarget.models.add(indexInTarget, model);
		canvasTarget.sendChanged(new AddedModelChange(indexInTarget, model), propCtx, propDistance, 0, branch);
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
		private HashSet<Model> shownModels = new HashSet<Model>();
		private Memoizer1<Model, Binding<ModelComponent>> modelToModelComponentMap;
		
		public CanvasPanel(final ModelComponent rootView, CanvasModel model, final TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			setLayout(null);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setOpaque(true);
			
			modelToModelComponentMap = new Memoizer1<Model, Binding<ModelComponent>>(new Func1<Model, Binding<ModelComponent>>() {
				@Override
				public Binding<ModelComponent> call(Model model) {
					final Binding<ModelComponent> modelView = model.createView(rootView, viewManager, transactionFactory.extend(new IndexLocator(CanvasPanel.this.model, model)));
					
					Rectangle bounds = new Rectangle(
						((Fraction)model.getProperty("X")).intValue(),
						((Fraction)model.getProperty("Y")).intValue(),
						((Fraction)model.getProperty("Width")).intValue(),
						((Fraction)model.getProperty("Height")).intValue()
					);
					
					((JComponent)modelView.getBindingTarget()).setBounds(bounds);
					
					return modelView;
				}
			});
		}

		@Override
		public Model getModelBehind() {
			return model;
		}
		
		@Override
		public void appendContainerTransactions(
				final LivePanel livePanel, TransactionMapBuilder transactions, final ModelComponent child, final PrevaylerServiceBranch<Model> branch) {
			transactions.addTransaction("Remove", new Runnable() {
				@Override
				public void run() {
					PropogationContext propCtx = new PropogationContext();
					branch.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							// Clear the current selection which is, here, assumed to the child
							livePanel.productionPanel.editPanelMouseAdapter.createSelectCommands(null, dualCommands);
							
							int indexOfModel = model.indexOfModel(child.getModelBehind());
							Location canvasLocation = transactionFactory.getModelLocation();
							
							// TODO: Make the backward transaction
							// The removed model should probably be reconstructed
							// The direct structure (clone isolated) (without observers and observees) could probably be used
							// where this direct structure should, afterwards, be decorated with any missing relations to observers and observees
							Model childClone = child.getModelBehind().cloneDeep(); // TODO: Fix this: Not a perfect clone
							Command<Model> backward = new AddModelNoCreationBoundsTransaction(canvasLocation, indexOfModel, new AsIsFactory(childClone));
							
							dualCommands.add(new DualCommandPair<Model>(
								new RemoveModelTransaction(canvasLocation, indexOfModel),
								backward
							));
						}
					});
				}
			});
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, transactionFactory, transactions, branch);
		}
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, branch);
		}
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent livePanel,
				final ModelComponent dropped, final Rectangle droppedBounds, final Point dropPoint, TransactionMapBuilder transactions, final PrevaylerServiceBranch<Model> branch) {
			if(dropped.getTransactionFactory().getParent() != null && 
				dropped.getTransactionFactory().getParent() != CanvasPanel.this.transactionFactory &&
				!isContainerOf(dropped.getTransactionFactory(), this.transactionFactory) /*Dropee cannot be child of dropped*/) {
				transactions.addTransaction("Move", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();
						branch.execute(propCtx, new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								ModelComponent modelToMove = dropped;
								ModelComponent target = CanvasPanel.this;
								appendMoveTransaction(dualCommands, (LivePanel)livePanel, modelToMove, target, droppedBounds.getLocation());
							}
						});
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
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			return null;
		}
		
		@Override
		public void initialize() {

		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			for(Component child: getComponents())
				((ModelComponent)child).visitTree(visitAction);
			
			visitAction.run(this);
		}
	}
	
	public static void appendMoveTransaction(List<DualCommand<Model>> dualCommands, LivePanel livePanel, ModelComponent modelToMove, ModelComponent target, final Point moveLocation) {
		Location canvasSourceLocation = modelToMove.getTransactionFactory().getParent().getModelLocation();
		ModelLocation canvasTargetLocation = target.getTransactionFactory().getModelLocation();
		
		int indexTarget = ((CanvasModel)target.getModelBehind()).getModelCount();
		CanvasModel sourceCanvas = (CanvasModel)ModelComponent.Util.getParent(modelToMove).getModelBehind();
		int indexSource = sourceCanvas.indexOfModel(modelToMove.getModelBehind());
		CanvasModel targetCanvas = (CanvasModel)target.getModelBehind();
		
		ModelLocation canvasTargetLocationAfter;
		int indexOfTargetCanvasInSource = sourceCanvas.indexOfModel(targetCanvas);
		if(indexOfTargetCanvasInSource != -1 && indexSource < indexOfTargetCanvasInSource) {
			// If target canvas is contained with the source canvas, then special care needs to be taken as
			// to predicting the location of target canvas after the move has taken place:
			// - If index of target canvas > index of model to be moved, then the predicated index of target canvas should 1 less
			int predictedIndexOfTargetCanvasInSource = indexOfTargetCanvasInSource - 1;
			canvasTargetLocationAfter = modelToMove.getTransactionFactory().getParent().extendLocation(new CanvasModel.IndexLocation(predictedIndexOfTargetCanvasInSource));
		} else {
			canvasTargetLocationAfter = canvasTargetLocation;
		}
		
		Location modelLocationAfterMove = new CompositeModelLocation(canvasTargetLocationAfter, new CanvasModel.IndexLocation(indexTarget));
		
		livePanel.productionPanel.editPanelMouseAdapter.createSelectCommands(null, dualCommands);
		
		dualCommands.add(new DualCommandPair<Model>(
			new CanvasModel.MoveModelTransaction(canvasSourceLocation, canvasTargetLocation, indexSource, indexTarget), 
			new CanvasModel.MoveModelTransaction(canvasTargetLocationAfter, canvasSourceLocation, indexTarget, indexSource)
		));
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(modelLocationAfterMove, "X", new Fraction(moveLocation.x)), 
			new Model.SetPropertyTransaction(modelLocationAfterMove, "X", modelToMove.getModelBehind().getProperty("X"))
		));
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(modelLocationAfterMove, "Y", new Fraction(moveLocation.y)), 
			new Model.SetPropertyTransaction(modelLocationAfterMove, "Y", modelToMove.getModelBehind().getProperty("Y"))
		));
		
		dualCommands.add(LiveModel.SetOutput.createDual(livePanel, modelLocationAfterMove));
	}
	
	public static class IndexLocator implements ModelLocator {
		private CanvasModel canvasModel;
		private Model model;

		public IndexLocator(CanvasModel canvasModel, Model model) {
			this.canvasModel = canvasModel;
			this.model = model;
		}

		@Override
		public ModelLocation locate() {
			int index = canvasModel.indexOfModel(model);
			return new IndexLocation(index);
		}
	}
	
	public static class IndexLocation implements ModelLocation {
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
		public Location getModelComponentLocation() {
			return new ViewIndexLocation(index);
		}
	}
	
	private static class ViewIndexLocation implements Location {
		private int index;
		
		public ViewIndexLocation(int index) {
			this.index = index;
		}

		@Override
		public Object getChild(Object holder) {
			// Is the model at index visible? If so, then return the corresponding model. If not, then return null.
			Model model = ((CanvasPanel)holder).model.models.get(index);
			return ((CanvasPanel)holder).modelToModelComponentMap.get(model).getBindingTarget();
		}
	}

	private void addModelComponent(
			final ModelComponent rootView, 
			final CanvasPanel view, final TransactionFactory transactionFactory, 
			final ViewManager viewManager, 
			Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap, final Model model,
			final Runner viewChangeRunner) {
		Integer viewModel2 = (Integer)model.getProperty(Model.PROPERTY_VIEW);
		if(viewModel2 == null)
			viewModel2 = 1;

		if(view.model.conformsToView(viewModel2)) {
			view.shownModels.add(model);
			
			final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);

			viewChangeRunner.run(new Runnable() {
				@Override
				public void run() {
					view.add((JComponent)modelView.getBindingTarget());
					view.setComponentZOrder((JComponent)modelView.getBindingTarget(), 0);
				}
			});
		}
		
		Model.RemovableListener removableListener = Model.RemovableListener.addObserver(model, new Observer() {
			@Override
			public void removeObservee(Observer observee) { }
			
			@Override
			public void changed(Model sender, Object change,
					PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						int modelView2 = (int)propertyChanged.value;
						if(view.model.conformsToView(modelView2)) {
							// Should be shown
							if(!view.shownModels.contains(sender)) {
								final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.add(sender);
								
								branch.onFinished(new Runnable() {
									@Override
									public void run() {
										view.add((JComponent)modelView.getBindingTarget());
									}
								});
								
								int zOrder = view.shownModels.size();
								for(int i = 0; i < models.size(); i++) {
									Model m = models.get(i);
									
									if(view.shownModels.contains(m))
										zOrder--;
									
									if(m == sender)
										break;
								}

								final int localZOrder = zOrder;
								branch.onFinished(new Runnable() {
									@Override
									public void run() {
										view.setComponentZOrder((JComponent)modelView.getBindingTarget(), localZOrder);
									}
								});
							}
						} else {
							// Should be hidden
							if(view.shownModels.contains(sender)) {
								final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.remove(sender);
								
								branch.onFinished(new Runnable() {
									@Override
									public void run() {
										view.remove((JComponent)modelView.getBindingTarget());
									}
								});
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
	public Binding<ModelComponent> createView(final ModelComponent rootView, final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final CanvasPanel view = new CanvasPanel(rootView, this, transactionFactory, viewManager);
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_BACKGROUND);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, view, view, viewManager, Model.COMPONENT_COLOR_BACKGROUND);
		Model.wrapForComponentGUIEvents(this, view, view, viewManager);
		
		final HashSet<Model> shownModels = new HashSet<Model>();
		
		final Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap = new Hashtable<Model, Model.RemovableListener>();
		for(final Model model: models) {
			addModelComponent(
				rootView, view, transactionFactory, viewManager, 
				modelToRemovableListenerMap, model,
				new Runner() {
					@Override
					public void run(Runnable runnable) {
						runnable.run();
					}
				}
			);
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, final PrevaylerServiceBranch<Model> branch) {
				if(change instanceof CanvasModel.AddedModelChange) {
					CanvasModel.AddedModelChange addedChange = (CanvasModel.AddedModelChange)change;
					final Model model = addedChange.model;
					
					addModelComponent(
						rootView, view, transactionFactory, viewManager, 
						modelToRemovableListenerMap, model,
						new Runner() {
							@Override
							public void run(Runnable runnable) {
								branch.onFinished(runnable);
							}
						}
					);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					// It could be possible to have map mapping from model to model component as follows:
					Model removedModel = ((CanvasModel.RemovedModelChange)change).model;
					
					Binding<ModelComponent> removedMCBinding = view.modelToModelComponentMap.get(removedModel);
					view.modelToModelComponentMap.clear(removedModel);
					final ModelComponent removedMC = removedMCBinding.getBindingTarget();
					
					Model.RemovableListener removableListener = modelToRemovableListenerMap.get(removedModel);
					removableListener.releaseBinding();
					
					branch.onFinished(new Runnable() {
						@Override
						public void run() {
							view.remove((JComponent)removedMC);
						}
					});
				} else if(change instanceof Model.PropertyChanged && propDistance == 1) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						Hashtable<Integer, Model> invisibles = new Hashtable<Integer, Model>();

						for(int i = 0; i < view.model.models.size(); i++) {
							Model m = view.model.models.get(i);
							boolean wasFound = false;
							for(Component mc: view.getComponents()) {
								if(m == ((ModelComponent)mc).getModelBehind()) {
									wasFound = true;
									break;
								}
							}
							if(!wasFound)
								invisibles.put(i, m);
						}
						
						Hashtable<Integer, Model> newVisibles = new Hashtable<Integer, Model>();

						for(Map.Entry<Integer, Model> entry: invisibles.entrySet()) {
							Model invisible = entry.getValue();
							if(invisible.viewConformsTo((int)propertyChanged.value)) {
								newVisibles.put(entry.getKey(), invisible);
							}
						}
						
						ArrayList<Component> newInvisibles = new ArrayList<Component>();
						for(Component mc: view.getComponents()) {
							if(!((ModelComponent)mc).getModelBehind().viewConformsTo((int)propertyChanged.value)) {
								newInvisibles.add(mc);
							}
						}
						
						for(final Component newInvisible: newInvisibles) {
							shownModels.remove(((ModelComponent)newInvisible).getModelBehind());
							
							branch.onFinished(new Runnable() {
								@Override
								public void run() {
									view.remove(newInvisible);
								}
							});
						}
						
						Object[] visibles = new Object[view.model.models.size()];
						for(Component mc: view.getComponents()) {
							boolean isVisible = shownModels.contains(mc);

							if(isVisible) {
								int indexOfVisible = view.model.indexOfModel(((ModelComponent)mc).getModelBehind());
								visibles[indexOfVisible] = mc;
							}
						}
						
						// Add the new visibles at each their respective index at model into visibles
						for(Map.Entry<Integer, Model> entry: newVisibles.entrySet()) {
							visibles[entry.getKey()] = entry.getValue();
						}
						
						for(int i = 0; i < visibles.length; i++) {
							Object visible = visibles[i];
							if(visible != null) {
								if(visible instanceof Model) {
									// Model to add
									Model model = (Model)visible;
									shownModels.add(model);
									final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);

									branch.onFinished(new Runnable() {
										@Override
										public void run() {
											view.add((JComponent)modelView.getBindingTarget());
										}
									});
								}
							}
						}
						
						int zOrder = shownModels.size();
						for(int i = 0; i < visibles.length; i++) {
							Object visible = visibles[i];
							if(visible != null) {
								zOrder--;
								if(visible instanceof Model) {
									// Model to add
									Model model = (Model)visible;
									final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
									
									final int localZOrder = zOrder;
									branch.onFinished(new Runnable() {
										@Override
										public void run() {
											view.setComponentZOrder((JComponent)modelView.getBindingTarget(), localZOrder);
										}
									});
								} else {
									final JComponent component = (JComponent)visibles[i];
									final int localZOrder = zOrder;
									branch.onFinished(new Runnable() {
										@Override
										public void run() {
											view.setComponentZOrder(component, localZOrder);
										}
									});
								}
							}
						}
					}
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
