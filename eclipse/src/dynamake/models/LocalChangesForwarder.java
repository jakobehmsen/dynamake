package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ReplayCommand;
import dynamake.commands.UnplayCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.Execution;
import dynamake.transcription.ExecutionsHandler;
import dynamake.transcription.SimplePendingCommandFactory;
import dynamake.transcription.Trigger;

/**
 * Instances each are supposed to forward change made in an source to an target.
 * The relation is not supposed to be one-to-one between source and target; instead
 * target are to support isolated changes which are maintained safely even when changes
 * are forwarded from the source.
 */
public class LocalChangesForwarder extends ObserverAdapter implements Serializable {
	public static class PushLocalChanges {
		public final Location offset;
		public final List<CommandState<Model>> localChangesToRevert;
		public final List<CommandState<Model>> newChanges;

		public PushLocalChanges(Location offset, List<CommandState<Model>> localChangesToRevert, List<CommandState<Model>> newChanges) {
			this.offset = offset;
			this.localChangesToRevert = localChangesToRevert;
			this.newChanges = newChanges;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model source;
	private Model target;
	
	public LocalChangesForwarder(Model source, Model target) {
		this.source = source;
		// at this point, target is assumed to be clone of source with no local changes
		this.target = target;
	}
	
	public void attach(PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}
	
	public Model getSource() {
		return source;
	}
	
	public Model getTarget() {
		return target;
	}
	
	public boolean forwardsTo(Model model) {
		return target == model;
	}
	
	public boolean forwardsFrom(Model model) {
		return source == model;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof LocalChangesForwarder) {
			LocalChangesForwarder localChangesForwarder = (LocalChangesForwarder)obj;
			return this.source == localChangesForwarder.source && this.target == localChangesForwarder.target;
		}
		return false;
	}
	
	private final List<Model> getModelsFromInnerToOuter(Model inner, Model outer) {
		// inner is assumed to child of outer either directly or indirectly
		
		ArrayList<Model> models = new ArrayList<Model>();
		Model currentModel = inner;
		
		while(currentModel != outer) {
			models.add(currentModel);
			currentModel = currentModel.getParent();
		}
		
		models.add(outer);
		
		return models;
	}
	
//	private final List<Location> getLocationsFromInnerToOuter(Model inner, Model outer, Location locationOfInner) {
//		// inner is assumed to child of outer either directly or indirectly
//		// locationOfInner is assumed to be the offset from outer to inner
//		
//		ArrayList<Location> locations = new ArrayList<Location>();
//		Location currentLocation = locationOfInner;
//		
//		while(currentLocation != outer) {
//			locations.add(currentLocation);
//			currentLocation = new CompositeLocation(currentLocation, new ParentLocation());
//		}
//		
//		locations.add(currentLocation);
//		
//		return locations;
//	}

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof PushLocalChanges && sender == source) {
//			System.out.println("***Forwarding from " + source + " to " + target + "***");
			
			// Whenever a change is forwarded from a source
			final PushLocalChanges pushLocalChanges = (PushLocalChanges)change;
			
			Location forwardedOffset = pushLocalChanges.offset.forForwarding();
			final Model innerMostTarget = (Model)CompositeLocation.getChild(this.target, new ModelRootLocation(), forwardedOffset);
			
			final ArrayList<CommandState<Model>> forwardedNewChanges = new ArrayList<CommandState<Model>>();
			
			// Forward new changes
			for(CommandState<Model> newChange: pushLocalChanges.newChanges)
				forwardedNewChanges.add(newChange.forForwarding());

			// Forward changes to revert
			final ArrayList<CommandState<Model>> forwardedChangesToRevert = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> newChange: pushLocalChanges.localChangesToRevert)
				forwardedChangesToRevert.add(newChange.forForwarding());

			// On a meta level (i.e. build commands which are not going to be part of the inheretee's local changes)
			collector.execute(new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final List<Model> modelsFromInnerToOuter = getModelsFromInnerToOuter(innerMostTarget, target);

					final ArrayList<Integer> modelIndexToLocationHistory = new ArrayList<Integer>();
					// Unplay from innermost part of target to outermost part of target
					// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
					for(int i = 0; i < modelsFromInnerToOuter.size(); i++) {
						Model currentTarget = modelsFromInnerToOuter.get(i);
						int localChangeCount = currentTarget.getLocalChangeCount();
						modelIndexToLocationHistory.add(localChangeCount);
						// Assumed that unplaying doesn't provoke side effects
						// Play the local changes backwards
						collector.execute(new SimplePendingCommandFactory<Model>(currentTarget, new PendingCommandState<Model>(
							new UnplayCommand(localChangeCount),
							new ReplayCommand(localChangeCount)
						)));
					}
					
					PendingCommandFactory.Util.executeSequence(collector, innerMostTarget, forwardedChangesToRevert, new ExecutionsHandler<Model>() {
						@Override
						public void handleExecutions(final List<Execution<Model>> forwardedChangesToRevertPendingUndoablePairs, Collector<Model> collector) {
							// Do the forwarded change without affecting the local changes
							// The forwarded changes must be, each, of type PendingCommandState
							final ArrayList<CommandState<Model>> forwardedNewChangesAsPendings = new ArrayList<CommandState<Model>>();
							for(CommandState<Model> forwardedNewChange: forwardedNewChanges)
								forwardedNewChange.appendPendings(forwardedNewChangesAsPendings);
							
							PendingCommandFactory.Util.executeSequence(collector, innerMostTarget, forwardedNewChangesAsPendings, new ExecutionsHandler<Model>() {
								@Override
								public void handleExecutions(List<Execution<Model>> forwardedNewChangesPendingUndoablePairs, Collector<Model> collector) {
									// Play the inherited local changes forwards without affecting the local changes
									ArrayList<CommandState<Model>> backwardOutput = new ArrayList<CommandState<Model>>();
									// They may not have been any forwarded changes to revert
									if(forwardedChangesToRevertPendingUndoablePairs != null) {
										for(Execution<Model> pup: forwardedChangesToRevertPendingUndoablePairs)
											backwardOutput.add(pup.undoable);
									}
									
									Collections.reverse(backwardOutput);

									PendingCommandFactory.Util.executeSequence(collector, innerMostTarget, backwardOutput, new ExecutionsHandler<Model>() {
										@Override
										public void handleExecutions(List<Execution<Model>> pendingUndoablePairs, Collector<Model> collector) {
											// Unplay from innermost part of target to outermost part of target
											// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
											for(int i = modelsFromInnerToOuter.size() - 1; i >= 0; i--) {
												Model currentTarget = modelsFromInnerToOuter.get(i);
												int localChangeCount = modelIndexToLocationHistory.get(i);
												modelIndexToLocationHistory.add(localChangeCount);
												// Play the local changes forward
												collector.execute(new SimplePendingCommandFactory<Model>(currentTarget, new PendingCommandState<Model>(
													new ReplayCommand(localChangeCount),
													new UnplayCommand(localChangeCount)
												)));
											}
										}
									});
								}
							});
						}
					});
				}
			});
			
			// Accumulate local changes to revert
			ArrayList<CommandState<Model>> newLocalChangesToRevert = new ArrayList<CommandState<Model>>();

			// Accumulate from innermost part of target to outermost part of target
			// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
			Location currentLocation = new ModelRootLocation();
			Model currentModel = innerMostTarget;
			
			while(true) {
				for(CommandState<Model> localChangeBackwards: currentModel.getLocalChangesBackwards())
					newLocalChangesToRevert.add(localChangeBackwards.offset(currentLocation));
				
				if(currentModel != this.target) {
					currentModel = currentModel.getParent();
					currentLocation = new CompositeLocation(currentLocation, new ParentLocation());
				} else {
					break;
				}
			}
			
//			newLocalChangesToRevert.addAll(innerMostTarget.getLocalChangesBackwards());
			
			newLocalChangesToRevert.addAll(forwardedChangesToRevert);
			
			this.target.sendChanged(new PushLocalChanges(forwardedOffset, newLocalChangesToRevert, forwardedNewChanges), propCtx, propDistance, changeDistance, collector);
		}
	}
}
