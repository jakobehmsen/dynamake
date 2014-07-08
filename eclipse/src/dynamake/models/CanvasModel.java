package dynamake.models;

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

import dynamake.caching.Memoizer1;
import dynamake.commands.Command;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.UnwrapTransaction;
import dynamake.commands.WrapTransaction;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.delegates.Runner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.AsIsFactory;
import dynamake.models.factories.Factory;
import dynamake.numbers.Fraction;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberOnFlush;
import dynamake.transcription.TranscriberRunnable;

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
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(prevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(prevalentSystem);
			Model model = (Model)canvasSource.getModel(indexInSource);

			int indexOfModel = canvasSource.indexOfModel(model);
			canvasSource.removeModel(indexOfModel, propCtx, 0, null, collector);
			canvasTarget.addModel(indexInTarget, model, propCtx, 0, null, collector);
		}
	}
	
	public static class AddModelTransaction implements Command<Model> {
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, propCtx, 0, collector);

			IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, isolatedCollector);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, isolatedCollector);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, isolatedCollector);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, isolatedCollector);
			
			canvas.addModel(model, new PropogationContext(), 0, null, collector);
		}
	}
	
	public static class AddModelAtTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		private Factory factory;
		private int index;
		
		public AddModelAtTransaction(Location canvasLocation, Rectangle creationBounds, Factory factory, int index) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, propCtx, 0, collector);

			IsolatingCollector<Model> isolatingCollector = new IsolatingCollector<>(collector);

			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, isolatingCollector);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, isolatingCollector);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, isolatingCollector);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, isolatingCollector);
			
			canvas.addModel(index, model, new PropogationContext(), 0, null, collector);
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, null, propCtx, 0, collector);
			canvas.addModel(index, model, new PropogationContext(), 0, null, collector);
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
			if(index < 0)
				new String();
			this.canvasLocation = canvasLocation;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModel(index);
			canvas.removeModel(index, propCtx, 0, null, collector);
			modelToRemove.beRemoved();
		}
	}
	
	public void addModel(Model model, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		addModel(models.size(), model, propCtx, propDistance, branch, collector);
	}

	public Model getModel(int index) {
		return models.get(index);
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		models.add(index, model);
		collector.registerAffectedModel(this);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, collector);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance, branch, collector);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		Model model = models.get(index);
		models.remove(index);
		collector.registerAffectedModel(this);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0, collector);
	}
	
	public static void move(CanvasModel canvasSource, CanvasModel canvasTarget, Model model, int indexInTarget, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		int indexOfModel = canvasSource.indexOfModel(model);
		canvasSource.models.remove(indexOfModel);
		canvasSource.sendChanged(new RemovedModelChange(indexOfModel, model), propCtx, propDistance, 0, collector);
		canvasTarget.models.add(indexInTarget, model);
		canvasTarget.sendChanged(new AddedModelChange(indexInTarget, model), propCtx, propDistance, 0, collector);
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
		private ModelTranscriber modelTranscriber;
		private HashSet<Model> shownModels = new HashSet<Model>();
		private Memoizer1<Model, Binding<ModelComponent>> modelToModelComponentMap;
		
		public CanvasPanel(final ModelComponent rootView, CanvasModel model, final ModelTranscriber modelTranscriber, final ViewManager viewManager) {
			this.model = model;
			this.modelTranscriber = modelTranscriber;
			setLayout(null);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setOpaque(true);
			
			modelToModelComponentMap = new Memoizer1<Model, Binding<ModelComponent>>(new Func1<Model, Binding<ModelComponent>>() {
				@Override
				public Binding<ModelComponent> call(Model model) {
					final Binding<ModelComponent> modelView = model.createView(rootView, viewManager, modelTranscriber.extend(new IndexLocator(CanvasPanel.this.model, model)));
					
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
				final LivePanel livePanel, CompositeMenuBuilder menuBuilder, final ModelComponent child) {
			menuBuilder.addMenuBuilder("Remove", new TranscriberRunnable<Model>() {
				@Override
				public void run(TranscriberCollector<Model> collector) {
					collector.execute(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location canvasLocation = modelTranscriber.getModelLocation();
							
							CanvasModel.appendRemoveTransaction(dualCommands, livePanel, child, canvasLocation, model);
						}
					});
				}
			});
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, modelTranscriber, menuBuilder, null);
			// The canvas model can be unwrap only if all the following cases are true:
			// - It has one ore more models contained in itself
			// - Its parent is a canvas model; i.e. canvases can only be unwrapped into other canvases
			if(model.models.size() > 0 && ModelComponent.Util.getParent(this).getModelBehind() instanceof CanvasModel) {
				menuBuilder.addMenuBuilder("Unwrap", new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.execute(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
//								ModelComponent parent = ModelComponent.Util.getParent(CanvasPanel.this);
//								CanvasModel target = (CanvasModel)parent.getModelBehind();
//								CanvasModel modelToBeUnwrapped = model;
//								Location targetLocation = parent.getModelTranscriber().getModelLocation();
//								int indexOfWrapper = target.indexOfModel(modelToBeUnwrapped);
//								ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
//								Rectangle creationBoundsInSelection = new Rectangle(
//									((Number)modelToBeUnwrapped.getProperty("X")).intValue(),
//									((Number)modelToBeUnwrapped.getProperty("Y")).intValue(),
//									((Number)modelToBeUnwrapped.getProperty("Width")).intValue(),
//									((Number)modelToBeUnwrapped.getProperty("Height")).intValue()
//								);
//								
//								// Each of the model locations should be moved from target to wrapper
//								Location[] modelLocations = new Location[modelToBeUnwrapped.models.size()];
//								for(int i = 0; i < modelLocations.length; i++) {
//									ModelComponent view = (ModelComponent)CanvasPanel.this.getComponent(i);
//									modelLocations[i] = view.getModelTranscriber().getModelLocation();
//								}
//								
//								dualCommands.add(new DualCommandPair<Model>(
//									new UnwrapTransaction(targetLocation, wrapperLocationInTarget, creationBoundsInSelection),
//									new WrapTransaction(targetLocation, creationBoundsInSelection, modelLocations)
//								));
								
								CanvasModel.appendUnwrapTransaction(dualCommands, CanvasPanel.this);
							}
						});
					}
				});
			}
		}
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder, null);
		}
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent livePanel,
				final ModelComponent dropped, final Rectangle droppedBounds, final Point dropPoint, CompositeMenuBuilder menuBuilder) {
			if(dropped.getModelTranscriber().getParent() != null && 
				dropped.getModelTranscriber().getParent() != CanvasPanel.this.modelTranscriber &&
				!isContainerOf(dropped.getModelTranscriber(), this.modelTranscriber) /*Dropee cannot be child of dropped*/) {
				menuBuilder.addMenuBuilder("Move", new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.execute(new DualCommandFactory<Model>() {
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
		
		private boolean isContainerOf(ModelTranscriber container, ModelTranscriber item) {
			ModelTranscriber parent = item.getParent();
			if(parent != null) {
				if(parent == container)
					return true;
				return isContainerOf(container, parent);
			}
			return false;
		}

		@Override
		public ModelTranscriber getModelTranscriber() {
			return modelTranscriber;
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
	
	public static void appendUnwrapTransaction(List<DualCommand<Model>> dualCommands, ModelComponent toUnwrap) {
		ModelComponent parent = ModelComponent.Util.getParent(toUnwrap);
		CanvasModel target = (CanvasModel)parent.getModelBehind();
		CanvasModel modelToBeUnwrapped = (CanvasModel)toUnwrap.getModelBehind();
		Location targetLocation = parent.getModelTranscriber().getModelLocation();
		int indexOfWrapper = target.indexOfModel(modelToBeUnwrapped);
		ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
		Rectangle creationBoundsInSelection = new Rectangle(
			((Number)modelToBeUnwrapped.getProperty("X")).intValue(),
			((Number)modelToBeUnwrapped.getProperty("Y")).intValue(),
			((Number)modelToBeUnwrapped.getProperty("Width")).intValue(),
			((Number)modelToBeUnwrapped.getProperty("Height")).intValue()
		);
		
		// Each of the model locations should be moved from target to wrapper
		Location[] modelLocations = new Location[modelToBeUnwrapped.models.size()];
		for(int i = 0; i < modelLocations.length; i++) {
			ModelComponent view = (ModelComponent)((JComponent)toUnwrap).getComponent(i);
			modelLocations[i] = view.getModelTranscriber().getModelLocation();
		}
		
		dualCommands.add(new DualCommandPair<Model>(
			new UnwrapTransaction(targetLocation, wrapperLocationInTarget, creationBoundsInSelection),
			new WrapTransaction(targetLocation, creationBoundsInSelection, modelLocations)
		));
	}
	
	public static void appendRemoveTransaction(List<DualCommand<Model>> dualCommands, LivePanel livePanel, ModelComponent child, Location canvasLocation, CanvasModel model) {
		int indexOfModel = model.indexOfModel(child.getModelBehind());
		
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
	
	public static void appendMoveTransaction(List<DualCommand<Model>> dualCommands, LivePanel livePanel, ModelComponent modelToMove, ModelComponent target, final Point moveLocation) {
		Location canvasSourceLocation = modelToMove.getModelTranscriber().getParent().getModelLocation();
		ModelLocation canvasTargetLocation = target.getModelTranscriber().getModelLocation();
		
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
			canvasTargetLocationAfter = modelToMove.getModelTranscriber().getParent().extendLocation(new CanvasModel.IndexLocation(predictedIndexOfTargetCanvasInSource));
		} else {
			canvasTargetLocationAfter = canvasTargetLocation;
		}
		
		Location modelLocationAfterMove = new CompositeModelLocation(canvasTargetLocationAfter, new CanvasModel.IndexLocation(indexTarget));
		
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
			final CanvasPanel view, final ModelTranscriber modelTranscriber, 
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
					PropogationContext propCtx, int propDistance, int changeDistance, TranscriberCollector<Model> collector) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						int modelView2 = (int)propertyChanged.value;
						if(view.model.conformsToView(modelView2)) {
							// Should be shown
							if(!view.shownModels.contains(sender)) {
								final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.add(sender);
								
								collector.afterNextFlush(new TranscriberOnFlush<Model>() {
									@Override
									public void run(TranscriberCollector<Model> collector) {
										view.add((JComponent)modelView.getBindingTarget());
									}
								});
								
								ArrayList<Model> shownModelsSequence = new ArrayList<Model>();
								
								for(int i = 0; i < models.size(); i++) {
									Model m = models.get(i);
									
									if(view.shownModels.contains(m))
										shownModelsSequence.add(m);
								}
								
								int zOrder = shownModelsSequence.size();
								for(int i = 0; i < shownModelsSequence.size(); i++) {
									zOrder--;
									
									Model m = shownModelsSequence.get(i);
									
									if(m == sender)
										break;
								}

								final int localZOrder = zOrder;
								collector.afterNextFlush(new TranscriberOnFlush<Model>() {
									@Override
									public void run(TranscriberCollector<Model> collector) {
										view.setComponentZOrder((JComponent)modelView.getBindingTarget(), localZOrder);
									}
								});
							}
						} else {
							// Should be hidden
							if(view.shownModels.contains(sender)) {
								final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.remove(sender);
								
								collector.afterNextFlush(new TranscriberOnFlush<Model>() {
									@Override
									public void run(TranscriberCollector<Model> collector) {
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
	public Binding<ModelComponent> createView(final ModelComponent rootView, final ViewManager viewManager, final ModelTranscriber modelTranscriber) {
		this.setLocator(modelTranscriber.getModelLocator());
		
		final CanvasPanel view = new CanvasPanel(rootView, this, modelTranscriber, viewManager);
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_BACKGROUND);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, view, view, viewManager, Model.COMPONENT_COLOR_BACKGROUND);
		Model.wrapForComponentGUIEvents(this, view, view, viewManager);
		
		final HashSet<Model> shownModels = new HashSet<Model>();
		
		final Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap = new Hashtable<Model, Model.RemovableListener>();
		for(final Model model: models) {
			addModelComponent(
				rootView, view, modelTranscriber, viewManager, 
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
			public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, final TranscriberCollector<Model> collector) {
				if(change instanceof CanvasModel.AddedModelChange) {
					CanvasModel.AddedModelChange addedChange = (CanvasModel.AddedModelChange)change;
					final Model model = addedChange.model;
					
					addModelComponent(
						rootView, view, modelTranscriber, viewManager, 
						modelToRemovableListenerMap, model,
						new Runner() {
							@Override
							public void run(final Runnable runnable) {
								collector.afterNextFlush(new TranscriberOnFlush<Model>() {
									@Override
									public void run(TranscriberCollector<Model> collector) {
										runnable.run();
									}
								});
							}
						}
					);
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					Model removedModel = ((CanvasModel.RemovedModelChange)change).model;
					
					Binding<ModelComponent> removedMCBinding = view.modelToModelComponentMap.get(removedModel);
					removedMCBinding.releaseBinding();
					view.modelToModelComponentMap.clear(removedModel);
					final ModelComponent removedMC = removedMCBinding.getBindingTarget();
					// Mark the model physically non-existent at this point in the current branch
					// (this may change before committing the branch)
					removedMC.getModelBehind().setLocator(null);
					
					Model.RemovableListener removableListener = modelToRemovableListenerMap.get(removedModel);
					removableListener.releaseBinding();
					
					collector.afterNextFlush(new TranscriberOnFlush<Model>() {
						public void run(dynamake.transcription.TranscriberCollector<Model> collector) {
							view.remove((JComponent)removedMC);
						}
					});
//					branch.onFinished(new Runnable() {
//						@Override
//						public void run() {
//							view.remove((JComponent)removedMC);
//						}
//					});
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
							
							collector.afterNextFlush(new TranscriberOnFlush<Model>() {
								@Override
								public void run(TranscriberCollector<Model> collector) {
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

									collector.afterNextFlush(new TranscriberOnFlush<Model>() {
										@Override
										public void run(TranscriberCollector<Model> collector) {
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
									collector.afterNextFlush(new TranscriberOnFlush<Model>() {
										@Override
										public void run(TranscriberCollector<Model> collector) {
											view.setComponentZOrder((JComponent)modelView.getBindingTarget(), localZOrder);
										}
									});
								} else {
									final JComponent component = (JComponent)visibles[i];
									final int localZOrder = zOrder;
									collector.afterNextFlush(new TranscriberOnFlush<Model>() {
										@Override
										public void run(TranscriberCollector<Model> collector) {
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
