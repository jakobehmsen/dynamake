package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PlayLocalChangesFromSourceCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int rootDistance;
	private Location locationOfSource;

	public PlayLocalChangesFromSourceCommand(Location locationOfSource) {
		this.rootDistance = 1;
		this.locationOfSource = locationOfSource;
	}

	private PlayLocalChangesFromSourceCommand(Location locationOfSource, int rootDistance) {
		this.rootDistance = rootDistance;
		this.locationOfSource = locationOfSource;
	}
	
	public PlayLocalChangesFromSourceCommand whereRootDistanceIs(int i) {
		return new PlayLocalChangesFromSourceCommand(locationOfSource, rootDistance + i);
	}
	
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfSource);
		Model target = (Model)location.getChild(prevalentSystem);
		
		List<CommandState<Model>> reversedForwardedChanges = playThenReverseChanges(source, target, propCtx, 0, collector);
		
//		System.out.println("Forwarded local changes from " + source + " to " + target);
		
		return new PlayThenReverseCommand(reversedForwardedChanges);
	}
	
	private List<CommandState<Model>> playThenReverseChanges(Model source, Model target, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		ArrayList<CommandState<Model>> mappedChangesToPush = new ArrayList<CommandState<Model>>();
		for(Model.PendingUndoablePair changeToPush: source.getLocalChangesAsPairs()) {
			mappedChangesToPush.add(changeToPush.forForwarding().pending);
//			changeToPush.toString();
		}
		
//		List<CommandState<Model>> reversedForwardedChanges = target.playThenReverse(source.getLocalChanges(), propCtx, propDistance, collector);
		List<CommandState<Model>> reversedForwardedChanges = target.playThenReverse(mappedChangesToPush, propCtx, propDistance, collector);
		
//		for(CommandState<Model> changeToForward: source.getLocalChangesWithOutput()) {
//			Model.UndoRedoPart urpChangeToForward = (Model.UndoRedoPart)changeToForward;
//			PendingCommandState<Model> pcsChangeToForward = (PendingCommandState<Model>)urpChangeToForward.origin;
//			if(pcsChangeToForward.getCommand() instanceof CanvasModel.AddModelCommand) {
//				CanvasModel.AddModelCommand.Output addModelOutput = (CanvasModel.AddModelCommand.Output)((ReversibleCommand<Model>)urpChangeToForward.revertible).getOutput();
//				Model embeddedSource = ((CanvasModel)source).getModelByLocation(addModelOutput.location);
//				Model embeddedTarget = ((CanvasModel)target).getModelByLocation(addModelOutput.location);
//				
//				List<CommandState<Model>> reversedEmbeddedForwardedChanges = playThenReverseChanges(embeddedSource, embeddedTarget, propCtx, propDistance, collector);
//				reversedForwardedChanges.addAll(reversedEmbeddedForwardedChanges);
//			}
//		}
		
		if(source instanceof CanvasModel) {
			CanvasModel sourceCanvas = (CanvasModel)source;
			CanvasModel targetCanvas = (CanvasModel)target;
			
			for(Location modelLocation: sourceCanvas.getLocations()) {
				Model embeddedSource = sourceCanvas.getModelByLocation(modelLocation);
//				if(!(modelLocation instanceof CanvasModel.ForwardLocation))
				// Forwarding should relative to the distance of the root
				// E.g., a target may be a descendant of a descendant of model/the root.
				// In this case, the distance to the root is 2.
				// Thus, each model local should be wrapped into 2 ForwardLocations
				for(int i = 0; i < rootDistance; i++)
					modelLocation = new CanvasModel.ForwardLocation(modelLocation);
				Model embeddedTarget = targetCanvas.getModelByLocation(modelLocation);
				
				List<CommandState<Model>> reversedEmbeddedForwardedChanges = playThenReverseChanges(embeddedSource, embeddedTarget, propCtx, propDistance, collector);
				reversedForwardedChanges.addAll(reversedEmbeddedForwardedChanges);
			}
		}
		
		return reversedForwardedChanges;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfSource);
		Location locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new PlayLocalChangesFromSourceCommand(locationOfSourceFromTargetReference);
	}
}
