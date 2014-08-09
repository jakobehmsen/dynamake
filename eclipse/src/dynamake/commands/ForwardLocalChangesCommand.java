package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.delegates.Func1;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.Observer;
import dynamake.models.PropogationContext;
import dynamake.models.LocalChangesForwarder.PushLocalChanges;
import dynamake.transcription.Collector;
import dynamake.transcription.Trigger;

public class ForwardLocalChangesCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location locationOfSource;

	public ForwardLocalChangesCommand(Location locationOfSource) {
		this.locationOfSource = locationOfSource;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfSource);
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
	
	private void pushForward(final Model source, final LocalChangesForwarder forwarder, final int distanceToTarget, final PropogationContext propCtx, final int propDistance, Collector<Model> collector) {
//		LocalChangesForwarder sourceForwarder = source.getObserveeOfType(LocalChangesForwarder.class);
		final LocalChangesForwarder sourceForwarder = (LocalChangesForwarder)source.getObserverWhere(new Func1<Observer, Boolean>() {
			@Override
			public Boolean call(Observer observer) {
				return observer instanceof LocalChangesForwarder &&
					((LocalChangesForwarder)observer).getTarget() == source;
			}
		});
		
		if(sourceForwarder != null) {
			collector.execute(new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					// Source is itself being forwarded changes
					// Push forward these changes before the change of this source
					pushForward(sourceForwarder.getSource(), forwarder, distanceToTarget + 1, propCtx, propDistance, collector);
				}
			});
//			// Source is itself being forwarded changes
//			// Push forward these changes before the change of this source
//			pushForward(sourceForwarder.getSource(), forwarder, distanceToTarget + 1, propCtx, propDistance, collector);
		}
		
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				// Push forward changes from source to target immediately through forwarder
				pushForward(source, forwarder, distanceToTarget, new ModelRootLocation(), propCtx, propDistance, collector);
			}
		});
		
//		// Push forward changes from source to target immediately through forwarder
//		pushForward(source, forwarder, distanceToTarget, new ModelRootLocation(), propCtx, propDistance, collector);
	}
	
	private void pushForward(Model source, final LocalChangesForwarder forwarder, final int distanceToTarget, final Location offset, final PropogationContext propCtx, final int propDistance, Collector<Model> collector) {
		ArrayList<CommandState<Model>> toForward = new ArrayList<CommandState<Model>>();

		// Is there some creation for source? Then this creation should also be (initially) forwarded
		// Creation must be a list of PendingUndoablePair.
		@SuppressWarnings("unchecked")
		List<Model.PendingUndoablePair> sourceCreation = (List<Model.PendingUndoablePair>)source.getProperty("Creation");
		if(sourceCreation != null) {
			for(Model.PendingUndoablePair sourceCreationPart: sourceCreation) {
				boolean forwardSourceCreationPart = false;
				PendingCommandState<Model> pending = (PendingCommandState<Model>)sourceCreationPart.pending;
				if(!(offset instanceof ModelRootLocation) && !(pending.getCommand() instanceof ForwardLocalChangesUpwards2Command)) {
					forwardSourceCreationPart = true;
//					toForward.add(sourceCreationPart); 
					// ForwardLocalChangesCommand should keep forwarding from the same source?
				} else if(!(pending.getCommand() instanceof ForwardLocalChangesCommand) && !(pending.getCommand() instanceof ForwardLocalChangesUpwards2Command)) {
					forwardSourceCreationPart = true;
				}
				
				if(forwardSourceCreationPart) {
					for(int i = 1; i < distanceToTarget; i++) {
						sourceCreationPart = sourceCreationPart.forForwarding();
					}
					
					toForward.add(sourceCreationPart);
				}
//				if(!(sourceCreationPart.pending.getCommand() instanceof ForwardLocalChangesCommand) && !(sourceCreationPart.pending.getCommand() instanceof ForwardLocalChangesUpwards2Command))
//					toForward.add(sourceCreationPart);
			}
		}

		// Why aren't the preceeding commands of toForward forwarded
		Location forwardedOffset = offset;
		for(int i = 1; i < distanceToTarget; i++) {
			forwardedOffset = forwardedOffset.forForwarding();
		}
		
		for(Model.PendingUndoablePair pup: source.getLocalChangesAsPairs()) {
			for(int i = 1; i < distanceToTarget; i++) {
				pup = pup.forForwarding();
			}
			toForward.add(pup);
		}
		
		if(toForward.size() > 0) {
			forwarder.changed(forwarder.getSource(), new PushLocalChanges(forwardedOffset, new ArrayList<CommandState<Model>>(), toForward), propCtx, propDistance, 0, collector);
		}
		
		if(source instanceof CanvasModel) {
			CanvasModel sourceCanvas = (CanvasModel)source;
			
			for(Location locationInSource: sourceCanvas.getLocations()) {
				final Model modelInSource = sourceCanvas.getModelByLocation(locationInSource);
				final Location newOffset = new CompositeLocation(offset, locationInSource);
				
				collector.execute(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						pushForward(modelInSource, forwarder, distanceToTarget, newOffset, propCtx, propDistance, collector);
					}
				});
//				pushForward(modelInSource, forwarder, distanceToTarget, newOffset, propCtx, propDistance, collector);
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
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfSource);
		Location locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new ForwardLocalChangesCommand(locationOfSourceFromTargetReference);
	}
}