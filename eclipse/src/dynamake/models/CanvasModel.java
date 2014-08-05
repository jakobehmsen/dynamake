package dynamake.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import dynamake.caching.Memoizer1;
import dynamake.commands.Command;
import dynamake.commands.CommandFactory;
import dynamake.commands.CommandState;
import dynamake.commands.ForwardableCommand;
import dynamake.commands.ForwardableCommandFactory;
import dynamake.commands.ForwardableOutput;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RelativeCommand;
import dynamake.commands.RewrapCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnwrapCommand;
import dynamake.delegates.Func1;
import dynamake.delegates.Runner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.ModelCreation;
import dynamake.models.factories.ModelFactory;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;
import dynamake.transcription.Trigger;

public class CanvasModel extends Model {
	private static class Entry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location id;
		public final Model model;
		
		public Entry(Location id, Model model) {
			this.id = id;
			this.model = model;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int nextId;
	private ArrayList<Entry> models;
	
	public CanvasModel() {
		models = new ArrayList<Entry>();
	}
	
	public CanvasModel(ArrayList<Entry> models) {
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
		for(Entry entry: models)
			entry.model.scale(hChange, vChange, propCtx, propDistance, collector);
	}
	
	@Override
	public Model modelCloneIsolated() {
		CanvasModel canvasClone = new CanvasModel(); 

		for(Entry entry: models) {
			Model modelClone = entry.model.cloneIsolated();
			canvasClone.models.add(new Entry(entry.id, modelClone));
			modelClone.setParent(canvasClone);
		}
		
		return canvasClone;
	}
	
	@Override
	protected void modelBeRemoved(PropogationContext propCtx, int propDistance, Collector<Model> collector, List<CommandState<Model>> restoreCommands) {
		for(Entry entry: models) {
			Location modelLocation = getLocationOf(entry.model);
			for(CommandState<Model> modelRestoreCommand: entry.model.getLocalChanges())
				restoreCommands.add(modelRestoreCommand.offset(modelLocation));
			ArrayList<CommandState<Model>> innerRestoreCommands = new ArrayList<CommandState<Model>>();
			entry.model.beRemoved(propCtx, propDistance, collector, innerRestoreCommands);
			for(CommandState<Model> innerRestoreCommand: innerRestoreCommands)
				restoreCommands.add(innerRestoreCommand.offset(modelLocation));
		}
	}
	
	@Override
	protected void modelAddContent(HashSet<Model> contained) {
		for(Entry entry: models) {
			entry.model.addContent(contained);
		}
	}

	public int getModelCount() {
		return models.size();
	}

	@Override
	protected void cloneAndMap(Hashtable<Model, Model> sourceToCloneMap) {
		CanvasModel canvasClone = new CanvasModel();
		canvasClone.properties = new Hashtable<String, Object>();
		// Assumed that cloning is not necessary for properties
		// I.e., all property values are immutable
		canvasClone.properties.putAll(this.properties);
		
		canvasClone.undoStack.addAll(this.undoStack);
		canvasClone.redoStack.addAll(this.redoStack);
		
		sourceToCloneMap.put(this, canvasClone);

		for(Entry entry: models) {
			entry.model.cloneAndMap(sourceToCloneMap);
			Model modelClone = sourceToCloneMap.get(entry.model);
			canvasClone.models.add(new Entry(entry.id, modelClone));
			modelClone.setParent(canvasClone);
		}
	}
	
	@Override
	public Model cloneBase() {
		return new CanvasModel();
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
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location canvasSourceLocation;
			public final Location canvasTargetLocation;
			public final Location movedFromInSource;
			public final Location movedToInTarget;
			
			public Output(Location canvasSourceLocation, Location canvasTargetLocation, Location movedFromInSource, Location movedToInTarget) {
				this.canvasSourceLocation = canvasSourceLocation;
				this.canvasTargetLocation = canvasTargetLocation;
				this.movedFromInSource = movedFromInSource;
				this.movedToInTarget = movedToInTarget;
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private Location locationInSource;

		public MoveModelCommand(Location canvasSourceLocation, Location canvasTargetLocation, Location locationInSource) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.locationInSource = locationInSource;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			CanvasModel canvasSource = (CanvasModel)CompositeLocation.getChild(prevalentSystem, location, canvasSourceLocation);
			CanvasModel canvasTarget = (CanvasModel)CompositeLocation.getChild(prevalentSystem, location, canvasTargetLocation);
			Model model = (Model)canvasSource.getModelByLocation(locationInSource);

			canvasSource.removeModelByLocation(locationInSource, propCtx, 0, collector);
			canvasTarget.addModel(model, propCtx, 0, collector);
			Location locationInTarget = canvasTarget.getLocationOf(model);
			
			return new Output(canvasSourceLocation, canvasTargetLocation, locationInSource, locationInTarget);
		}
	}
	
	public static class MoveBackModelCommand implements Command<Model> {
		public static class AfterMove implements CommandFactory<Model> {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command<Model> createCommand(Object output) {
				MoveModelCommand.Output moveOutput = (MoveModelCommand.Output)output;
				return new MoveBackModelCommand(moveOutput.canvasTargetLocation, moveOutput.canvasSourceLocation, moveOutput.movedToInTarget, moveOutput.movedFromInSource);
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private Location locationInSource;
		private Location locationInTarget;

		public MoveBackModelCommand(Location canvasSourceLocation, Location canvasTargetLocation, Location locationInSource, Location locationInTarget) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.locationInSource = locationInSource;
			this.locationInTarget = locationInTarget;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			CanvasModel canvasSource = (CanvasModel)CompositeLocation.getChild(prevalentSystem, location, canvasSourceLocation);
			CanvasModel canvasTarget = (CanvasModel)CompositeLocation.getChild(prevalentSystem, location, canvasTargetLocation);
			Model model = (Model)canvasSource.getModelByLocation(locationInSource);

			canvasSource.removeModelByLocation(locationInSource, propCtx, 0, collector);
			canvasTarget.restoreModelByLocation(locationInTarget, model, propCtx, 0, collector);
			
			return new MoveModelCommand.Output(canvasSourceLocation, canvasTargetLocation, locationInSource, locationInTarget);
		}
	}
	
	public static class AddModelCommand implements ForwardableCommand<Model> {
		public static class Output implements Serializable, ForwardableOutput {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location location;

			public Output(Location location) {
				this.location = location;
			}

			@Override
			public Object forForwarding() {
				return new AddModelCommand.Output(new ForwardLocation(location));
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final ModelFactory factory;
		
		public AddModelCommand(ModelFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location location) {
			final CanvasModel canvas = (CanvasModel)location.getChild(rootPrevalentSystem);
			ModelCreation modelCreation = factory.create(rootPrevalentSystem, propCtx, 0, collector, location);
			final Model model = modelCreation.createModel(rootPrevalentSystem, propCtx, 0, collector, location);

			IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
			
			canvas.addModel(model, new PropogationContext(), 0, collector);
			Location addedModelLocation = canvas.getLocationOf(model);
			
			modelCreation.setup(rootPrevalentSystem, model, addedModelLocation, propCtx, 0, isolatedCollector, location);
			
			return new Output(addedModelLocation);
		}
		
		@Override
		public Command<Model> forForwarding(Object output) {
			// When a model is added to a canvas, map id to ForwardedId (if not only already ForwardedId)
			// When a model is removed from a canvas, map id to ForwardedId (if not only already ForwardedId)
			AddModelCommand.Output addModelOutput = (AddModelCommand.Output)output;
			
			Location mappedLocation = new CanvasModel.ForwardLocation(addModelOutput.location);
			CanvasModel.ForwardedAddModelCommand newAddCommand = new CanvasModel.ForwardedAddModelCommand(mappedLocation, this.factory);

			return newAddCommand;
		}
	}
	
	public static class ForwardedAddModelCommand implements ForwardableCommand<Model> {
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location location;

			public Output(Location location) {
				this.location = location;
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location modelLocation;
		public final ModelFactory factory;
		
		public ForwardedAddModelCommand(Location modelLocation, ModelFactory factory) {
			this.modelLocation = modelLocation;
			this.factory = factory;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location location) {
			final CanvasModel canvas = (CanvasModel)location.getChild(rootPrevalentSystem);
			ModelCreation modelCreation = factory.create(rootPrevalentSystem, propCtx, 0, collector, location);
			final Model model = modelCreation.createModel(rootPrevalentSystem, propCtx, 0, collector, location);

			IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
			
			canvas.restoreModel(modelLocation, model, new PropogationContext(), 0, collector);
			Location addedModelLocation = canvas.getLocationOf(model);
			
			modelCreation.setup(rootPrevalentSystem, model, addedModelLocation, propCtx, 0, isolatedCollector, location);
			
			return new Output(addedModelLocation);
		}
		
		@Override
		public Command<Model> forForwarding(Object output) {
			// When a model is added to a canvas, map id to ForwardedId (if not only already ForwardedId)
			// When a model is removed from a canvas, map id to ForwardedId (if not only already ForwardedId)
			AddModelCommand.Output addModelOutput = (AddModelCommand.Output)output;
			
			Location mappedLocation = new CanvasModel.ForwardLocation(addModelOutput.location);
			CanvasModel.ForwardedAddModelCommand newAddCommand = new CanvasModel.ForwardedAddModelCommand(mappedLocation, this.factory);

			return newAddCommand;
		}
	}
	
	// Should be MappableCommand due to RestorableModel?
	public static class RestoreModelCommand implements Command<Model>, Cloneable {
		public static final class AfterRemove implements ForwardableCommandFactory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command<Model> createCommand(Object output) {
				RestorableModel restorableModel = ((RemoveModelCommand.Output)output).restorableModel;
				Location location = ((RemoveModelCommand.Output)output).location;
				// TODO: Consider the following:
				// What if the model what observing/being observed before its removal?
				// What if the model's observers/observees aren't all in existence anymore?
				// What if the model's observers/observees are restored after this model is restored?
				// Are all of the above cases possible?
				// Perhaps, the best solution would be to save the history and replay this history?
				
				return new CanvasModel.RestoreModelCommand(location, restorableModel);
			}
			
			@Override
			public CommandFactory<Model> forForwarding(Object output) {
				return new RestoreModelCommand.ForwardAfterRemove(this);
			}
		}
		
		public static final class ForwardAfterRemove implements ForwardableCommandFactory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private CommandFactory<Model> factory;

			public ForwardAfterRemove(CommandFactory<Model> factory) {
				this.factory = factory;
			}

			@Override
			public Command<Model> createCommand(Object output) {
				RestorableModel restorableModel = ((RemoveModelCommand.Output)output).restorableModel;
				
				Location mappedLocation = new CanvasModel.ForwardLocation(((RemoveModelCommand.Output)output).location);
				
				return factory.createCommand(new RemoveModelCommand.Output(mappedLocation, restorableModel));
			}
			
			@Override
			public CommandFactory<Model> forForwarding(Object output) {
				return new RestoreModelCommand.ForwardAfterRemove(this);
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocationToRestore;
		private RestorableModel restorableModel;
		
		public RestoreModelCommand(Location modelLocationToRestore, RestorableModel restorableModel) {
			this.modelLocationToRestore = modelLocationToRestore;
			this.restorableModel = restorableModel;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			CanvasModel canvas = (CanvasModel)location.getChild(prevalentSystem);
			
			Model modelBase = restorableModel.unwrapBase(propCtx, 0, collector);
			restorableModel.restoreOriginsOnBase(modelBase, propCtx, 0, collector);
			
			canvas.restoreModelByLocation(modelLocationToRestore, modelBase, new PropogationContext(), 0, collector);
			
			restorableModel.restoreChangesOnBase(modelBase, propCtx, 0, collector);
			restorableModel.restoreCleanupOnBase(modelBase, propCtx, 0, collector);
			
			return new AddModelCommand.Output(modelLocationToRestore);
		}
	}
	
	public static class RemoveModelCommand implements ForwardableCommand<Model> {
		public static class Output implements Serializable, ForwardableOutput {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location location;
			public final RestorableModel restorableModel;

			public Output(Location location, RestorableModel restorableModel) {
				this.location = location;
				this.restorableModel = restorableModel;
			}
			
			@Override
			public Object forForwarding() {
				return new RemoveModelCommand.Output(new ForwardLocation(location), restorableModel);
			}
		}
		
		public static final class AfterAdd implements ForwardableCommandFactory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command<Model> createCommand(Object output) {
				return new CanvasModel.RemoveModelCommand(((CanvasModel.AddModelCommand.Output)output).location);
			}
			
			@Override
			public CommandFactory<Model> forForwarding(Object output) {
				return new RemoveModelCommand.ForwardAfterAdd(this);
			}
		}
		
		public static final class ForwardAfterAdd implements ForwardableCommandFactory<Model>  
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private CommandFactory<Model> factory;

			public ForwardAfterAdd(CommandFactory<Model> factory) {
				this.factory = factory;
			}

			@Override
			public Command<Model> createCommand(Object output) {
				AddModelCommand.Output addModelOutput = (AddModelCommand.Output)output;
				
				Location mappedLocation = new CanvasModel.ForwardLocation(addModelOutput.location);
				
				return factory.createCommand(new AddModelCommand.Output(mappedLocation));
			}
			
			@Override
			public CommandFactory<Model> forForwarding(Object output) {
				return new RemoveModelCommand.ForwardAfterAdd(this);
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location locationOfModelToRemove;
		
		public RemoveModelCommand(Location locationOfModelToRemove) {
			this.locationOfModelToRemove = locationOfModelToRemove;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			CanvasModel canvas = (CanvasModel)location.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModelByLocation(locationOfModelToRemove);
			
			@SuppressWarnings("unchecked")
			List<CommandState<Model>> restoreCommands = (List<CommandState<Model>>)modelToRemove.getProperty(RestorableModel.PROPERTY_CREATION);
			modelToRemove.beRemoved(propCtx, 0, collector, restoreCommands);
			
			canvas.removeModelByLocation(locationOfModelToRemove, propCtx, 0, collector);
			
			RestorableModel restorableModel = RestorableModel.wrap(modelToRemove, true);
			
			return new Output(locationOfModelToRemove, restorableModel);
		}
		
		@Override
		public Command<Model> forForwarding(Object output) {
			// When a model is added to a canvas, map id to ForwardedId (if not only already ForwardedId)
			// When a model is removed from a canvas, map id to ForwardedId (if not only already ForwardedId)
			RemoveModelCommand.Output removeModelOutput = (RemoveModelCommand.Output)output;
			
			Location mappedLocation = new CanvasModel.ForwardLocation(removeModelOutput.location);
			CanvasModel.RemoveModelCommand newRemoveCommand = new CanvasModel.RemoveModelCommand(mappedLocation);

			return newRemoveCommand;
		}
	}
	
	private void restoreModel(Location id, Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int index = models.size();
		models.add(index, new Entry(id, model));
		model.setParent(this);
		collector.registerAffectedModel(this);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, collector);
	}

	public void restoreModelByLocation(Location location, Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		restoreModel(location, model, propCtx, propDistance, collector);
	}

	public void removeModelByLocation(Location location, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// TODO: Figure out: How to support ForwardLocations here?
		int indexOf = getIndexOfModelById(location);
		removeModel(indexOf, propCtx, propDistance, collector);
		
//		System.out.println("Removed model with id " + ((IdLocation)location).id + " in canvas " + this);
	}

	public void addModel(Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		addModel(models.size(), model, propCtx, propDistance, collector);
	}

	public Model getModel(int index) {
		return models.get(index).model;
	}
	
	/**
	 * location is assumed to be an id itself.
	 * 
	 * @param location
	 * @return
	 */
	public Model getModelByLocation(Location location) {
		for(Entry entry: models) {
			if(entry.id.equals(location))
				return entry.model;
		}
		return null;
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int id = nextId++;
		
		models.add(index, new Entry(new IdLocation(id), model));
		model.setParent(this);
		collector.registerAffectedModel(this);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, collector);
		
//		System.out.println("Add model with id " + id + " in canvas " + this);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance, collector);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Model model = models.get(index).model;
		models.remove(index);
		model.setParent(null);
		collector.registerAffectedModel(this);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0, collector);
	}
	
	public int indexOfModel(Model model) {
		for(int i = 0; i < models.size(); i++) {
			if(models.get(i).model == model)
				return i;
		}
		
		return -1;
	}

	private int getIndexOfModelById(Location id) {
		for(int i = 0; i < models.size(); i++) {
			if(models.get(i).id.equals(id))
				return i;
		}
		
		return -1;
	}
	
	public Location getLocationOf(Model model) {
		int indexOfModel = indexOfModel(model);
		return models.get(indexOfModel).id;
	}

	public Location getNextLocation() {
		return new IdLocation(nextId);
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
					final Binding<ModelComponent> modelView = model.createView(rootView, viewManager, modelTranscriber.extend(new ItemLocator(CanvasPanel.this.model, model)));
					
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
					collector.execute(new PendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return model;
						}
						
						@Override
						public void createPendingCommands(List<CommandState<Model>> commandStates) {
							CanvasModel.appendRemoveTransaction(commandStates, livePanel, child, model);
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
			if(model.getModelCount() > 0 && ModelComponent.Util.getParent(this).getModelBehind() instanceof CanvasModel) {
				menuBuilder.addMenuBuilder("Unwrap", new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.execute(new PendingCommandFactory<Model>() {
							ModelComponent parent;
							
							@Override
							public Model getReference() {
								// If this canvas is unwrapped, then it is unwrapped into its parent
								parent = ModelComponent.Util.getParent(CanvasPanel.this);
								return parent.getModelBehind();
							}
							
							@Override
							public void createPendingCommands(List<CommandState<Model>> commandStates) {
								CanvasModel.appendUnwrapTransaction(commandStates, CanvasPanel.this, parent);
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

						// Reference is closest common ancestor
						collector.execute(new PendingCommandFactory<Model>() {
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
							public void createPendingCommands(List<CommandState<Model>> commandStates) {
								Location locationOfSource = ModelComponent.Util.locationFromAncestor(referenceMC, source);
								Location locationOfTarget = ModelComponent.Util.locationFromAncestor(referenceMC, targetOver);
								
								CanvasModel.appendMoveTransaction(commandStates, (LivePanel)livePanel, source, modelToMove, targetOver, droppedBounds.getLocation(), locationOfSource, locationOfTarget);
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
	}
	
	public static void appendUnwrapTransaction(List<CommandState<Model>> commandStates, ModelComponent toUnwrap, ModelComponent parent) {
		CanvasModel target = (CanvasModel)parent.getModelBehind();
		CanvasModel modelToBeUnwrapped = (CanvasModel)toUnwrap.getModelBehind();
		Location wrapperLocationInTarget = target.getLocationOf(modelToBeUnwrapped);
		RectangleF creationBoundsInSelection = new RectangleF(
			(Fraction)modelToBeUnwrapped.getProperty("X"),
			(Fraction)modelToBeUnwrapped.getProperty("Y"),
			(Fraction)modelToBeUnwrapped.getProperty("Width"),
			(Fraction)modelToBeUnwrapped.getProperty("Height")
		);
		
		commandStates.add(new PendingCommandState<Model>(
			new UnwrapCommand(wrapperLocationInTarget, creationBoundsInSelection),
			new RewrapCommand.AfterUnwrap(),
			new UnwrapCommand.AfterWrap()
		));
	}
	
	public static void appendRemoveTransaction(List<CommandState<Model>> commandStates, LivePanel livePanel, ModelComponent child, CanvasModel model) {
		Location locationOfModel = model.getLocationOf(child.getModelBehind());
		
		commandStates.add(new PendingCommandState<Model>(
			new RemoveModelCommand(locationOfModel),
			new RestoreModelCommand.AfterRemove(),
			new RemoveModelCommand.AfterAdd()
		));
	}
	
	public static void appendMoveTransaction(List<CommandState<Model>> commandStates, LivePanel livePanel, ModelComponent source, ModelComponent modelToMove, ModelComponent target, final Point moveLocation, Location canvasSourceLocation, Location canvasTargetLocation) {
		CanvasModel sourceCanvas = (CanvasModel)source.getModelBehind();
		Location locationInSource = sourceCanvas.getLocationOf(modelToMove.getModelBehind());
		
		Location canvasTargetLocationAfter = canvasTargetLocation;
		
		Location modelLocationAfterMove = new CompositeLocation(canvasTargetLocationAfter, ((CanvasModel)target.getModelBehind()).getNextLocation());
		
		commandStates.add(new PendingCommandState<Model>(
			new CanvasModel.MoveModelCommand(canvasSourceLocation, canvasTargetLocation, locationInSource), 
			new CanvasModel.MoveBackModelCommand.AfterMove(),
			new CanvasModel.MoveBackModelCommand.AfterMove()
		));
		
		commandStates.add(new PendingCommandState<Model>(
			new RelativeCommand<Model>(modelLocationAfterMove, new SetPropertyCommand("X", new Fraction(moveLocation.x))),
			new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
		));
		
		commandStates.add(new PendingCommandState<Model>(
			new RelativeCommand<Model>(modelLocationAfterMove, new SetPropertyCommand("Y", new Fraction(moveLocation.y))),
			new RelativeCommand.Factory<Model>(new SetPropertyCommand.AfterSetProperty())
		));
	}
	
	private static class ItemLocator implements Locator {
		private CanvasModel canvasModel;
		private Model model;

		public ItemLocator(CanvasModel canvasModel, Model model) {
			this.canvasModel = canvasModel;
			this.model = model;
		}

		@Override
		public Location locate() {
			return canvasModel.getLocationOf(model);
		}
	}
	
	public static class IdLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Object id;

		public IdLocation(Object id) {
			this.id = id;
		}
		
		@Override
		public Object getChild(Object holder) {
			return ((CanvasModel)holder).getModelByLocation(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof IdLocation && this.id.equals(((IdLocation)obj).id);
		}
		
		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
	
	public static class ForwardLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location location;

		public ForwardLocation(Location location) {
			this.location = location;
		}
		
		@Override
		public Object getChild(Object holder) {
			return ((CanvasModel)holder).getModelByLocation(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof ForwardLocation && this.location.equals(((ForwardLocation)obj).location);
		}
		
		@Override
		public int hashCode() {
			return location.hashCode() * 15;
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
								
								for(int i = 0; i < getModelCount(); i++) {
									Model m = getModel(i);
									
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

	public Model getModelById(Object id) {
		for(Entry entry: models) {
			if(entry.id.equals(id))
				return entry.model;
		}
		
		return null;
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
		for(final Entry entry: models) {
			addModelComponent(
				rootView, view, modelTranscriber, viewManager, 
				modelToRemovableListenerMap, entry.model,
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

						for(int i = 0; i < view.model.getModelCount(); i++) {
							Model m = view.model.getModel(i);
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
						
						Object[] visibles = new Object[view.model.getModelCount()];
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

	public Location[] getLocations() {
		Location[] locations = new Location[getModelCount()];
		for(int i = 0; i < getModelCount(); i++) {
			Entry entry = getEntry(i);
			locations[i] = entry.id;
		}
		return locations;
	}

	private Entry getEntry(int index) {
		return models.get(index);
	}
}
