package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.ForwardHistoryCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnforwardHistoryCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class NewInstanceFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public NewInstanceFactory(RectangleF creationBounds, Location modelLocation) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
	}
	
	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
				Location instanceLocation = new CompositeLocation(location, locationOfModelToSetup);
				Model instance = (Model)instanceLocation.getChild(rootModel);
				
				@SuppressWarnings("unchecked")
				List<CommandState<Model>> changesToInheret = (List<CommandState<Model>>)instance.getProperty("Inhereted");
				
				Location locationOfInhereterFromInstance = ModelComponent.Util.locationBetween(instance, inhereter);
				
				// TODO: Consider: How to make instance cloneable?
				// The path to inhereter from inheretee must be derived somehow?...
				// It could be relative to root
				// Perhaps, the dropped/inhereter could be supplied for the play sequence of cloning/unwrapping?
				// Perhaps, each command, during unwrapping, should be mapped to their equivalent in a new context?
				
				// Should be available during cloning: but not for new instances
				// Filter in history?
				ArrayList<CommandState<Model>> newChangesToInheret = new ArrayList<CommandState<Model>>();
				newChangesToInheret.add(new PendingCommandState<Model>(
					new ForwardHistoryCommand(locationOfInhereterFromInstance), 
					new UnforwardHistoryCommand(locationOfInhereterFromInstance)
				));
				changesToInheret.addAll(newChangesToInheret);

				// TODO: Consider: Inherit cleanup?
				List<CommandState<Model>> cleanup = instance.playThenReverse(newChangesToInheret, propCtx, propDistance, collector);
				instance.setProperty("Cleanup", cleanup, propCtx, propDistance, collector);
				
//				if(inhereter instanceof CanvasModel)
//					forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				final Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
				final Model instance = inhereter.cloneBase();

				@SuppressWarnings("unchecked")
				List<CommandState<Model>> origins = (List<CommandState<Model>>)inhereter.getProperty("Origins");
				instance.playThenReverse(origins, propCtx, propDistance, collector);
				instance.setProperty("Origins", origins, propCtx, propDistance, collector);
				
				// Inheritance of changes should be a command in itself
				// This is especially useful in restore scenarios where an inhereter of an inheretee being restored may have changed.
				ArrayList<CommandState<Model>> changesToInheret = new ArrayList<CommandState<Model>>();
				@SuppressWarnings("unchecked")
				List<CommandState<Model>> inhereterInheretedChanges = (List<CommandState<Model>>)inhereter.getProperty("Inhereted");
				if(inhereterInheretedChanges != null) {
					// Don't include ForwardHistoryCommand commands in changes to inheret 
					// TODO: Remove the ugly filter hack below; replace with decoupled logic
					for(CommandState<Model> inhereterInheretedChange: inhereterInheretedChanges) {
						PendingCommandState<Model> pcsInhereterInheretedChange = (PendingCommandState<Model>)inhereterInheretedChange;
						if(pcsInhereterInheretedChange.getCommand() instanceof ForwardHistoryCommand) {
							
						} else
							changesToInheret.add(inhereterInheretedChange);
					}
				}
				List<CommandState<Model>> inhereterLocalChanges = inhereter.getLocalChanges();
				changesToInheret.addAll(inhereterLocalChanges);

				// Map changesToInheret from inhereter (sourceReference) to instance (targetReference)
				for(int i = 0; i < changesToInheret.size(); i++) {
					CommandState<Model> newCommandState = changesToInheret.get(i).mapToReferenceLocation(inhereter, instance);
					changesToInheret.set(i, newCommandState);
				}
				
				// TODO: Consider: Inherit and map cleanup?

				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));

				instance.playThenReverse(changesToInheret, propCtx, propDistance, collector);
				instance.setProperty("Inhereted", changesToInheret, propCtx, propDistance, collector);
				
				return instance;
			}
		};
	}
	
//	@Override
//	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
//		Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
//		Location instanceLocation = new CompositeLocation(location, locationOfModelToSetup);
//		Model instance = (Model)instanceLocation.getChild(rootModel);
//		
////		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, instance);
////		inhereter.addObserver(historyChangeForwarder);
////		instance.addObserver(historyChangeForwarder);
////		historyChangeForwarder.attach(propCtx, propDistance, collector);
//		
////		ArrayList<CommandState<Model>> changesToInheret = new ArrayList<CommandState<Model>>();
////		@SuppressWarnings("unchecked")
////		List<CommandState<Model>> inhereterInheretedChanges = (List<CommandState<Model>>)inhereter.getProperty("Inhereted");
////		if(inhereterInheretedChanges != null)
////			changesToInheret.addAll(inhereterInheretedChanges);
////		List<CommandState<Model>> inhereterLocalChanges = inhereter.getLocalChanges();
////		changesToInheret.addAll(inhereterLocalChanges);
////
////		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
////		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
////		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
////		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
//		
//		@SuppressWarnings("unchecked")
//		List<CommandState<Model>> changesToInheret = (List<CommandState<Model>>)instance.getProperty("Inhereted");
//		
//		Location locationOfInhereterFromInstance = ModelComponent.Util.locationBetween(instance, inhereter);
//		
//		// TODO: Consider: How to make instance cloneable?
//		// The path to inhereter from inheretee must be derived somehow?...
//		// It could be relative to root
//		// Perhaps, the dropped/inhereter could be supplied for the play sequence of cloning/unwrapping?
//		// Perhaps, each command, during unwrapping, should be mapped to their equivalent in a new context?
//		changesToInheret.add(new PendingCommandState<Model>(new ForwardHistoryCommand(locationOfInhereterFromInstance, new ModelRootLocation()), new SetPropertyCommand.AfterSetProperty()));
//
//		instance.playThenReverse(changesToInheret, propCtx, propDistance, collector);
////		instance.setProperty("Inhereted", changesToInheret, propCtx, propDistance, collector);
//		
//		if(inhereter instanceof CanvasModel)
//			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
//	}
//	
//	private void forwardHistoryChangesToContainedModels(CanvasModel inhereterCanvas, CanvasModel inhereteeCanvas, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		for(Location location: inhereterCanvas.getLocations()) {
//			Model inhereterModel = inhereterCanvas.getModelByLocation(location);
//			Model inhereteeModel = inhereteeCanvas.getModelByLocation(location);
//
//			HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereterModel, inhereteeModel);
//			inhereterModel.addObserver(historyChangeForwarder);
//			inhereteeModel.addObserver(historyChangeForwarder);
//			historyChangeForwarder.attach(propCtx, propDistance, collector);
//			if(inhereterModel instanceof CanvasModel)
//				forwardHistoryChangesToContainedModels((CanvasModel)inhereterModel, (CanvasModel)inhereterModel, propCtx, propDistance, collector);
//		}
//	}
}
