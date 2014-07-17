package dynamake.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
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
import dynamake.commands.Command2;
import dynamake.commands.Command2Factory;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RelativeCommand;
import dynamake.commands.UnwrapCommand;
import dynamake.commands.UnwrapCommand2;
import dynamake.commands.UnwrapToLocationsCommand2;
import dynamake.commands.WrapCommand;
import dynamake.commands.WrapCommand2;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.delegates.Runner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.AsIsFactory;
import dynamake.models.factories.Factory;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;
import dynamake.transcription.Trigger;

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
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(Model model: models)
			model.scale(hChange, vChange, propCtx, propDistance, collector);
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
		
		clone.undoStack.addAll(this.undoStack);
		clone.redoStack.addAll(this.redoStack);
		
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
	
	public static class MoveModelCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private int indexInSource;
		private int indexInTarget;

		public MoveModelCommand(Location canvasSourceLocation, Location canvasTargetLocation, int indexInSource, int indexInTarget) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.indexInSource = indexInSource;
			this.indexInTarget = indexInTarget;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(prevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(prevalentSystem);
			Model model = (Model)canvasSource.getModel(indexInSource);

			int indexOfModel = canvasSource.indexOfModel(model);
			canvasSource.removeModel(indexOfModel, propCtx, 0, collector);
			canvasTarget.addModel(indexInTarget, model, propCtx, 0, collector);
		}
	}
	
	public static class MoveModelCommand2 implements Command2<Model> {
		public static class AfterMove implements Command2Factory<Model> {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command2<Model> createCommand(Object output) {
				MoveModelCommand2.Output moveOutput = (MoveModelCommand2.Output)output;
				return new MoveModelCommand2(moveOutput.canvasTargetLocation, moveOutput.canvasSourceLocation, moveOutput.movedToIndex);
			}
		}
		
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location canvasSourceLocation;
			public final Location canvasTargetLocation;
			public final int movedToIndex;
			
			public Output(Location canvasSourceLocation, Location canvasTargetLocation, int movedToIndex) {
				this.canvasSourceLocation = canvasSourceLocation;
				this.canvasTargetLocation = canvasTargetLocation;
				this.movedToIndex = movedToIndex;
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private int indexInSource;

		public MoveModelCommand2(Location canvasSourceLocation, Location canvasTargetLocation, int indexInSource) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.indexInSource = indexInSource;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			CanvasModel canvasSource = (CanvasModel)CompositeModelLocation.getChild(prevalentSystem, location, canvasSourceLocation);
			CanvasModel canvasTarget = (CanvasModel)CompositeModelLocation.getChild(prevalentSystem, location, canvasTargetLocation);
			Model model = (Model)canvasSource.getModel(indexInSource);

			int indexOfModel = canvasSource.indexOfModel(model);
			canvasSource.removeModel(indexOfModel, propCtx, 0, collector);
			canvasTarget.addModel(model, propCtx, 0, collector);
			int movedToIndex = canvasTarget.indexOfModel(model);
			
			return new Output(canvasSourceLocation, canvasTargetLocation, movedToIndex);
		}
	}
	
	public static class AddModelCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		private Factory factory;
		
		public AddModelCommand(Location canvasLocation, Rectangle creationBounds, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, propCtx, 0, collector, null);

			IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, isolatedCollector);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, isolatedCollector);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, isolatedCollector);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, isolatedCollector);
			
			canvas.addModel(model, new PropogationContext(), 0, collector);
		}
	}
	
	public static class AddModelCommand2 implements Command2<Model> {
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final int index;

			public Output(int index) {
				this.index = index;
			}
		}
		
		public static final class AfterRemove implements Command2Factory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command2<Model> createCommand(Object output) {
				Model model = ((RemoveModelCommand2.Output)output).model;
				// TODO: Consider the following:
				// What if the model what observing/being observed before its removal?
				// What if the model's observers/observees aren't all in existence anymore?
				// What if the model's observers/observees are restored after this model is restored?
				// Are all of the above cases possible?
				// Perhaps, the best solution would be to save the history and replay this history?
				Fraction x = (Fraction)model.getProperty("X");
				Fraction y = (Fraction)model.getProperty("Y");
				Fraction width = (Fraction)model.getProperty("Width");
				Fraction height = (Fraction)model.getProperty("Height");
				
				return new CanvasModel.AddModelCommand2(x, y, width, height, new AsIsFactory(model));
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Fraction xCreation;
		private Fraction yCreation;
		private Fraction widthCreation;
		private Fraction heightCreation;
		private Factory factory;
		
		public AddModelCommand2(Fraction xCreation, Fraction yCreation, Fraction widthCreation, Fraction heightCreation, Factory factory) {
			this.xCreation = xCreation;
			this.yCreation = yCreation;
			this.widthCreation = widthCreation;
			this.heightCreation = heightCreation;
			this.factory = factory;
		}
		
		public AddModelCommand2(Rectangle creationBounds, Factory factory) {
			this.xCreation = new Fraction(creationBounds.x);
			this.yCreation = new Fraction(creationBounds.y);
			this.widthCreation = new Fraction(creationBounds.width);
			this.heightCreation = new Fraction(creationBounds.height);
			this.factory = factory;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			CanvasModel canvas = (CanvasModel)location.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, propCtx, 0, collector, location);

			IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
			model.setProperty("X", xCreation, propCtx, 0, isolatedCollector);
			model.setProperty("Y", yCreation, propCtx, 0, isolatedCollector);
			model.setProperty("Width", widthCreation, propCtx, 0, isolatedCollector);
			model.setProperty("Height", heightCreation, propCtx, 0, isolatedCollector);
			
			canvas.addModel(model, new PropogationContext(), 0, collector);
			
			int index = canvas.getModelCount() - 1;
			
			return new Output(index);
		}
	}
	
	public static class AddModelAtCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		private Factory factory;
		private int index;
		
		public AddModelAtCommand(Location canvasLocation, Rectangle creationBounds, Factory factory, int index) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, propCtx, 0, collector, null);

			IsolatingCollector<Model> isolatingCollector = new IsolatingCollector<>(collector);

			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, isolatingCollector);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, isolatingCollector);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, isolatingCollector);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, isolatingCollector);
			
			canvas.addModel(index, model, new PropogationContext(), 0, collector);
		}
	}
	
	public static class AddModelNoCreationBoundsCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private int index;
		private Factory factory;
		
		public AddModelNoCreationBoundsCommand(Location canvasLocation, int index, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.index = index;
			this.factory = factory;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, propCtx, 0, collector, null);
			canvas.addModel(index, model, new PropogationContext(), 0, collector);
		}
	}
	
	public static class RemoveModelCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private int index;
		
		public RemoveModelCommand(Location canvasLocation, int index) {
			if(index < 0)
				new String();
			this.canvasLocation = canvasLocation;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModel(index);
			canvas.removeModel(index, propCtx, 0, collector);
			modelToRemove.beRemoved();
		}
	}
	
	public static class RemoveModelCommand2 implements Command2<Model> {
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final int index;
//			public final byte[] serialization;
			public final Model model;

			public Output(int index, Model model) {
				this.index = index;
				this.model = model;
			}
		}
		
		public static final class AfterAdd implements Command2Factory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command2<Model> createCommand(Object output) {
				return new CanvasModel.RemoveModelCommand2(((CanvasModel.AddModelCommand2.Output)output).index);
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public RemoveModelCommand2(int index) {
			if(index < 0)
				new String();
			this.index = index;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			CanvasModel canvas = (CanvasModel)location.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModel(index);
			canvas.removeModel(index, propCtx, 0, collector);
			modelToRemove.beRemoved();
			
			return new Output(index, modelToRemove);
		}
	}
	
	public void addModel(Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		addModel(models.size(), model, propCtx, propDistance, collector);
	}

	public Model getModel(int index) {
		return models.get(index);
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		models.add(index, model);
		collector.registerAffectedModel(this);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, collector);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance, collector);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model model = models.get(index);
		models.remove(index);
		collector.registerAffectedModel(this);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0, collector);
	}
	
	public static void move(CanvasModel canvasSource, CanvasModel canvasTarget, Model model, int indexInTarget, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int indexOfModel = canvasSource.indexOfModel(model);
		canvasSource.models.remove(indexOfModel);
		canvasSource.sendChanged(new RemovedModelChange(indexOfModel, model), propCtx, propDistance, 0, collector);
		canvasTarget.models.add(indexInTarget, model);
		canvasTarget.sendChanged(new AddedModelChange(indexInTarget, model), propCtx, propDistance, 0, collector);
	}
	
	public int indexOfModel(Model model) {
		return models.indexOf(model);
	}
	
	public ModelLocation getLocationOf(Model model) {
		int indexOfModel = indexOfModel(model);
		return new IndexLocation(indexOfModel);
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
			menuBuilder.addMenuBuilder("Remove", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
//					collector.execute(new DualCommandFactory<Model>() {
//						@Override
//						public Model getReference() {
//							return model;
//						}
//						
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							CanvasModel.appendRemoveTransaction(dualCommands, livePanel, child, location, model);
//						}
//					});
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return model;
						}
						
						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
							CanvasModel.appendRemoveTransaction2(commandStates, livePanel, child, model);
						}
					});
				}
			});
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, modelTranscriber, menuBuilder);
			// The canvas model can be unwrap only if all the following cases are true:
			// - It has one ore more models contained in itself
			// - Its parent is a canvas model; i.e. canvases can only be unwrapped into other canvases
			if(model.models.size() > 0 && ModelComponent.Util.getParent(this).getModelBehind() instanceof CanvasModel) {
				menuBuilder.addMenuBuilder("Unwrap", new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
//						collector.execute(new DualCommandFactory<Model>() {
//							ModelComponent parent;
//							
//							@Override
//							public Model getReference() {
//								// If this canvas is unwrapped, then it is unwrapped into its parent
//								parent = ModelComponent.Util.getParent(CanvasPanel.this);
//								return parent.getModelBehind();
//							}
//							
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								CanvasModel.appendUnwrapTransaction(dualCommands, CanvasPanel.this, parent, location);
//							}
//						});
						
						collector.execute(new CommandStateFactory<Model>() {
							ModelComponent parent;
							
							@Override
							public Model getReference() {
								// If this canvas is unwrapped, then it is unwrapped into its parent
								parent = ModelComponent.Util.getParent(CanvasPanel.this);
								return parent.getModelBehind();
							}
							
							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								CanvasModel.appendUnwrapTransaction2(commandStates, CanvasPanel.this, parent);
							}
						});
					}
				});
			}
		}
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder);
		}
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent livePanel,
				final ModelComponent dropped, final Rectangle droppedBounds, final Point dropPoint, CompositeMenuBuilder menuBuilder) {
			if(dropped.getModelTranscriber().getParent() != null && 
				dropped.getModelTranscriber().getParent() != CanvasPanel.this.modelTranscriber &&
				!isContainerOf(dropped.getModelTranscriber(), this.modelTranscriber) /*Dropee cannot be child of dropped*/) {
				menuBuilder.addMenuBuilder("Move", new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						final ModelComponent modelToMove = dropped;
						
//						// Reference is closest common ancestor
//						collector.execute(new DualCommandFactory<Model>() {
//							ModelComponent source;
//							ModelComponent targetOver;
//							ModelComponent referenceMC;
//							
//							@Override
//							public Model getReference() {
//								ModelComponent.Util.getParent(modelToMove);
//								targetOver = CanvasPanel.this;
//								referenceMC = ModelComponent.Util.closestCommonAncestor(source, targetOver);
//								return referenceMC.getModelBehind();
//							}
//
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								ModelLocation locationOfSource = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, source);
//								ModelLocation locationOfTarget = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, targetOver);
//								
//								CanvasModel.appendMoveTransaction(dualCommands, (LivePanel)livePanel, source, modelToMove, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget);
//							}
//						});
						
						// Reference is closest common ancestor
						collector.execute(new CommandStateFactory<Model>() {
							ModelComponent source;
							ModelComponent targetOver;
							ModelComponent referenceMC;
							
							@Override
							public Model getReference() {
								source = ModelComponent.Util.getParent(modelToMove);
								targetOver = CanvasPanel.this;
								referenceMC = ModelComponent.Util.closestCommonAncestor(source, targetOver);
								return referenceMC.getModelBehind();
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								ModelLocation locationOfSource = ModelComponent.Util.locationFromAncestor(referenceMC, source);
								ModelLocation locationOfTarget = ModelComponent.Util.locationFromAncestor(referenceMC, targetOver);
								
								CanvasModel.appendMoveTransaction2(commandStates, (LivePanel)livePanel, source, modelToMove, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget);
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
		public void initialize() {

		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			for(Component child: getComponents())
				((ModelComponent)child).visitTree(visitAction);
			
			visitAction.run(this);
		}
	}
	
	public static void appendUnwrapTransaction2(List<CommandState<Model>> commandStates, ModelComponent toUnwrap, ModelComponent parent) {
		CanvasModel target = (CanvasModel)parent.getModelBehind();
		CanvasModel modelToBeUnwrapped = (CanvasModel)toUnwrap.getModelBehind();
		int indexOfWrapper = target.indexOfModel(modelToBeUnwrapped);
		ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
		RectangleF creationBoundsInSelection = new RectangleF(
			(Fraction)modelToBeUnwrapped.getProperty("X"),
			(Fraction)modelToBeUnwrapped.getProperty("Y"),
			(Fraction)modelToBeUnwrapped.getProperty("Width"),
			(Fraction)modelToBeUnwrapped.getProperty("Height")
		);
//		
//		// Derive the an array of the locations at which the unwrapped models are to be placed
//		// within the target canvas
//		int indexOfModelInTarget = target.getModelCount();
//		if(target.indexOfModel(modelToBeUnwrapped) != -1)
//			indexOfModelInTarget--; // Decrement, since modelToBeUnwrapped will be removed
//		Location[] modelLocations = new Location[modelToBeUnwrapped.models.size()];
//		for(int i = 0; i < modelLocations.length; i++) {
//			Location viewLocation = new CanvasModel.IndexLocation(indexOfModelInTarget);
//			modelLocations[i] = viewLocation;
//			indexOfModelInTarget++;
//		}
		
//		dualCommands.add(new DualCommandPair<Model>(
//			new UnwrapCommand(targetLocation, wrapperLocationInTarget, creationBoundsInSelection),
//			new WrapCommand(targetLocation, creationBoundsInSelection, modelLocations)
//		));
		
		commandStates.add(new PendingCommandState<Model>(
			new UnwrapCommand2(wrapperLocationInTarget, creationBoundsInSelection),
			new WrapCommand2.AfterUnwrap(),
			new UnwrapCommand2.AfterWrap()
		));
		
//		commandStates.add(new PendingCommandState<Model>(
//			new UnwrapCommand2(wrapperLocationInTarget, creationBoundsInSelection),
//			new WrapCommand2(creationBoundsInSelection, modelLocations)
////				new WrapCommand2(new RectangleF(creationBoundsInSelection), modelLocations), 
////				new UnwrapToLocationsCommand2.AfterWrap(),
////				new WrapCommand2.AfterUnwrap()
//		));
	}
	
	public static void appendRemoveTransaction2(List<CommandState<Model>> commandStates, LivePanel livePanel, ModelComponent child, CanvasModel model) {
		int indexOfModel = model.indexOfModel(child.getModelBehind());
		
		commandStates.add(new PendingCommandState<Model>(
			new RemoveModelCommand2(indexOfModel),
			new AddModelCommand2.AfterRemove(),
			new RemoveModelCommand2.AfterAdd()
		));
	}
	
	public static void appendMoveTransaction2(List<CommandState<Model>> commandStates, LivePanel livePanel, ModelComponent source, ModelComponent modelToMove, ModelComponent target, final Point moveLocation, ModelLocation canvasSourceLocation, ModelLocation canvasTargetLocation) {
		int indexTarget = ((CanvasModel)target.getModelBehind()).getModelCount();
		CanvasModel sourceCanvas = (CanvasModel)source.getModelBehind();
		int indexSource = sourceCanvas.indexOfModel(modelToMove.getModelBehind());
		CanvasModel targetCanvas = (CanvasModel)target.getModelBehind();
		
		ModelLocation canvasTargetLocationAfter;
		int indexOfTargetCanvasInSource = sourceCanvas.indexOfModel(targetCanvas);
		if(indexOfTargetCanvasInSource != -1 && indexSource < indexOfTargetCanvasInSource) {
			// If target canvas is contained with the source canvas, then special care needs to be taken as
			// to predicting the location of target canvas after the move has taken place:
			// - If index of target canvas > index of model to be moved, then the predicated index of target canvas should 1 less
			int predictedIndexOfTargetCanvasInSource = indexOfTargetCanvasInSource - 1;
			canvasTargetLocationAfter = new CompositeModelLocation(canvasSourceLocation, new CanvasModel.IndexLocation(predictedIndexOfTargetCanvasInSource));
		} else {
			canvasTargetLocationAfter = canvasTargetLocation;
		}
		
		Location modelLocationAfterMove = new CompositeModelLocation(canvasTargetLocationAfter, new CanvasModel.IndexLocation(indexTarget));
		
		commandStates.add(new PendingCommandState<Model>(
			new CanvasModel.MoveModelCommand2(canvasSourceLocation, canvasTargetLocation, indexSource), 
			new CanvasModel.MoveModelCommand2.AfterMove(),
			new CanvasModel.MoveModelCommand2.AfterMove()
		));
		
		commandStates.add(new PendingCommandState<Model>(
			new RelativeCommand<Model>(modelLocationAfterMove, new Model.SetPropertyCommand2("X", new Fraction(moveLocation.x))),
			new RelativeCommand.Factory<Model>(new Model.SetPropertyCommand2.AfterSetProperty())
		));
		
		commandStates.add(new PendingCommandState<Model>(
			new RelativeCommand<Model>(modelLocationAfterMove, new Model.SetPropertyCommand2("Y", new Fraction(moveLocation.y))),
			new RelativeCommand.Factory<Model>(new Model.SetPropertyCommand2.AfterSetProperty())
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
			/*
			Instead of using indexes to locate models, id's relative to the canvas should be used.
			This is not so much due to the potential efficiency gains in complex canvases, but
			more due to effectiveness. More specifically, it is due to models being moved across
			canvases meaning the index-lookup quickly becomes ineffective from an identity viewpoint.
			By using id's, which are unique to canvases, it may be possible to track down models, by
			keeping of the models moved out of canvases (their id's) and where to and their id in the
			target canvases.
			If the index is coupled with a particular version of the canvas, it may function as a
			unique identifier, though. 
			*/
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
					PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						int modelView2 = (int)propertyChanged.value;
						if(view.model.conformsToView(modelView2)) {
							// Should be shown
							if(!view.shownModels.contains(sender)) {
								final Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.add(sender);
								
								collector.afterNextTrigger(new Runnable() {
									@Override
									public void run() {
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
								collector.afterNextTrigger(new Runnable() {
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
								
								collector.afterNextTrigger(new Runnable() {
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
			public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, final Collector<Model> collector) {
				if(change instanceof CanvasModel.AddedModelChange) {
					CanvasModel.AddedModelChange addedChange = (CanvasModel.AddedModelChange)change;
					final Model model = addedChange.model;
					
					addModelComponent(
						rootView, view, modelTranscriber, viewManager, 
						modelToRemovableListenerMap, model,
						new Runner() {
							@Override
							public void run(final Runnable runnable) {
								collector.afterNextTrigger(new Runnable() {
									@Override
									public void run() {
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
					
					collector.afterNextTrigger(new Runnable() {
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
							
							collector.afterNextTrigger(new Runnable() {
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

									collector.afterNextTrigger(new Runnable() {
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
									collector.afterNextTrigger(new Runnable() {
										@Override
										public void run() {
											view.setComponentZOrder((JComponent)modelView.getBindingTarget(), localZOrder);
										}
									});
								} else {
									final JComponent component = (JComponent)visibles[i];
									final int localZOrder = zOrder;
									collector.afterNextTrigger(new Runnable() {
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
