package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class InheritLocalChangesCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location locationOfInhereter;

	public InheritLocalChangesCommand(Location locationOfInhereter) {
		this.locationOfInhereter = locationOfInhereter;
	}
	
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)location.getChild(prevalentSystem);
		
//		HistoryChangeForwarder forwarder = inhereter.getObserverOfLike(new HistoryChangeForwarder(inhereter, inheretee));
//		
//		forwarder.changed(inhereter, new HistoryChangeForwarder.ForwardLogChange(new ArrayList<CommandState<Model>>(), inhereter.getLocalChanges()), propCtx, 0, 0, collector);
		
//		ArrayList<CommandState<Model>> filteredInheritedChanges = new ArrayList<CommandState<Model>>();
		
		List<CommandState<Model>> reversedInheritedChanges = playThenReverseChanges(inhereter, inheretee, propCtx, 0, collector);
		
//		List<CommandState<Model>> inheritedChanges = inhereter.getLocalChangesWithOutput();
//		
//		for(CommandState<Model> inheritedChange: inheritedChanges) {
//			Model.UndoRedoPart urpInheritedChange = (Model.UndoRedoPart)inheritedChange;
//			PendingCommandState<Model> pcsInheritedChange = (PendingCommandState<Model>)urpInheritedChange.origin;
//			if(pcsInheritedChange.getCommand() instanceof CanvasModel.AddModelCommand) {
//				CanvasModel.AddModelCommand.Output addModelOutput = (CanvasModel.AddModelCommand.Output)((ReversibleCommand<Model>)urpInheritedChange.revertible).getOutput();
//				Model inhereterChild = ((CanvasModel)inhereter).getModelByLocation(addModelOutput.location);
//				
//				// Traverse through child's local history and offset local changes with model location
//			} else {
//				filteredInheritedChanges.add(inheritedChange);
//			}
//		}
		
		// Is it possible to traverse through the inherited changes and find all add commands (like above)
		// and then replace the add methods with off set commands relative to the added model's commands?
		// Can this be done recursively, such that the added model's commands are processed the same?
		// I.e.; recursively expand add command with the added model's commands
		
//		List<CommandState<Model>> reversedInheritedChanges = inheretee.playThenReverse(inhereter.getLocalChanges(), propCtx, 0, collector);
		
		System.out.println(inheretee + " inherited from " + inhereter);
		
		return new PlayThenReverseCommand(reversedInheritedChanges);
		
//		return null;
	}
	
	private List<CommandState<Model>> playThenReverseChanges(Model inhereter, Model inheretee, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		List<CommandState<Model>> reversedInheritedChanges = inheretee.playThenReverse(inhereter.getLocalChanges(), propCtx, propDistance, collector);
		
		for(CommandState<Model> inheritedChange: inhereter.getLocalChangesWithOutput()) {
			Model.UndoRedoPart urpInheritedChange = (Model.UndoRedoPart)inheritedChange;
			PendingCommandState<Model> pcsInheritedChange = (PendingCommandState<Model>)urpInheritedChange.origin;
			if(pcsInheritedChange.getCommand() instanceof CanvasModel.AddModelCommand) {
				CanvasModel.AddModelCommand.Output addModelOutput = (CanvasModel.AddModelCommand.Output)((ReversibleCommand<Model>)urpInheritedChange.revertible).getOutput();
				Model inhereterChild = ((CanvasModel)inhereter).getModelByLocation(addModelOutput.location);
				Model inhereteeChild = ((CanvasModel)inheretee).getModelByLocation(addModelOutput.location);
				
				List<CommandState<Model>> reversedInheritedChildChanges = playThenReverseChanges(inhereterChild, inhereteeChild, propCtx, propDistance, collector);
				reversedInheritedChanges.addAll(reversedInheritedChildChanges);
			}
		}
		
		return reversedInheritedChanges;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model inhereter = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfInhereter);
		Location locationOfInhereterFromTargetReference = ModelComponent.Util.locationBetween(targetReference, inhereter);
		
		return new InheritLocalChangesCommand(locationOfInhereterFromTargetReference);
	}
}
