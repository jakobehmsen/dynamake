package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesForwarder;
import dynamake.models.LocalChangesUpwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.ParentLocation;
import dynamake.models.PropogationContext;
import dynamake.models.RootModel;
import dynamake.transcription.Collector;

public class ForwardLocalChangesCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location locationOfTarget;

	public ForwardLocalChangesCommand(Location locationOfSource) {
		this.locationOfTarget = locationOfSource;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfTarget);
		Model target = (Model)location.getChild(prevalentSystem);
		
		LocalChangesForwarder historyChangeForwarder = new LocalChangesForwarder(source, target);
		source.addObserver(historyChangeForwarder);
		target.addObserver(historyChangeForwarder);
		historyChangeForwarder.attach(propCtx, 0, collector);
		
//		// Should be done recursively upwards in the inheritance chain
//		List<CommandState<Model>> changesToInheret = inhereter.getLocalChanges();
//		inheretee.playThenReverse(changesToInheret, propCtx, 0, collector);
		
//		if(source instanceof CanvasModel)
//			forwardLocalChangesUpwards(historyChangeForwarder, (CanvasModel)source, new ModelRootLocation(), new ModelRootLocation());
		
		pushForward(source, historyChangeForwarder, 1, propCtx, 0, collector);
		
		System.out.println("Forward local changes from " + source + " to " + target);
		
		return null;
	}
	
	private void pushForward(Model source, LocalChangesForwarder forwarder, int distanceToTarget, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		LocalChangesForwarder sourceForwarder = source.getObserveeOfType(LocalChangesForwarder.class);
		
		if(sourceForwarder != null) {
			// Source is itself being forwarded changes
			// Push forward these changes before the change of this source
			pushForward(sourceForwarder.getSource(), forwarder, distanceToTarget + 1, propCtx, propDistance, collector);
		}
		
		// Push forward changes from source to target immediately through forwarder
		pushForward(source, forwarder, distanceToTarget, new ModelRootLocation(), propCtx, propDistance, collector);
	}
	
	private void pushForward(Model source, LocalChangesForwarder forwarder, int distanceToTarget, Location offset, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		ArrayList<Model.PendingUndoablePair> toForward = new ArrayList<Model.PendingUndoablePair>();

		// Is there some creation for source? Then this creation should also be (initially) forwarded
		// Creation must be a list of PendingUndoablePair.
		List<Model.PendingUndoablePair> creation = (List<Model.PendingUndoablePair>)source.getProperty("Creation");
		if(creation != null) {
			toForward.addAll(creation);
		}
		
		for(Model.PendingUndoablePair pup: source.getLocalChangesAsPairs()) {
			for(int i = 1; i < distanceToTarget; i++)
				pup = pup.forForwarding();
			pup = pup.offset(offset);
			toForward.add(pup);
		}
		
		forwarder.changed(source, new Model.HistoryAppendLogChange(toForward), propCtx, propDistance, 0, collector);
		
		if(source instanceof CanvasModel) {
			CanvasModel sourceCanvas = (CanvasModel)source;
			
			for(Location locationInSource: sourceCanvas.getLocations()) {
				Model modelInSource = sourceCanvas.getModelByLocation(locationInSource);
				Location newOffset = new CompositeLocation(offset, locationInSource);
				
				pushForward(modelInSource, forwarder, distanceToTarget, newOffset, propCtx, propDistance, collector);
			}
		}
	}
	
//	private void forwardLocalChangesUpwards(LocalChangesForwarder historyChangeForwarder, CanvasModel sourceCanvas, Location sourceLocation, Location offsetFromTarget) {
//		for(Location modelLocationInSource: sourceCanvas.getLocations()) {
//			Location modelLocationInTarget = new CanvasModel.ForwardLocation(modelLocationInSource);
//			// Perhaps, the creation of this upwards forwarding should be part for play local changes from source command, for each add command?
//			// - and then a corresponding cleanup for each remove command?
//			Model modelInSource = sourceCanvas.getModelByLocation(modelLocationInSource);
//			Location modelTargetLocation = new CompositeLocation(sourceLocation, new ParentLocation());
//			Location modelOffsetFromTarget = new CompositeLocation(offsetFromTarget, modelLocationInTarget);
//			modelInSource.addObserver(new LocalChangesUpwarder(modelTargetLocation, modelOffsetFromTarget));
//			
//			if(modelInSource instanceof CanvasModel)
//				forwardLocalChangesUpwards(historyChangeForwarder, (CanvasModel)modelInSource, modelTargetLocation, modelOffsetFromTarget);
//		}
//	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfTarget);
		Location locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new ForwardLocalChangesCommand(locationOfSourceFromTargetReference);
	}
}