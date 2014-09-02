package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.ExecuteContinuationsFromScopeCommand;
import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ReplayCommand;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.TriStatePURCommand;
import dynamake.commands.UnplayCommand;
import dynamake.models.transcription.ContinueTransactionHandler;
import dynamake.models.transcription.ContinueTransactionHandlerFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.NullTransactionHandler;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.Execution;
import dynamake.transcription.ExecutionsHandler;
import dynamake.transcription.SimplePendingCommandFactory;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.Trigger;
import dynamake.tuples.Tuple2;

/**
 * Instances each are supposed to forward change made in an source to an target.
 * The relation is not supposed to be one-to-one between source and target; instead
 * target are to support isolated changes which are maintained safely even when changes
 * are forwarded from the source.
 */
public class LocalChangesForwarder extends ObserverAdapter implements Serializable {
	public static class PushLocalChanges {
		public final Location<Model> offset;
		public final List<Tuple2<ExecutionScope<Model>, PURCommand<Model>>> localChangesToRevert;
		public final List<PURCommand<Model>> newChanges;

		public PushLocalChanges(Location<Model> offset, List<Tuple2<ExecutionScope<Model>, PURCommand<Model>>> localChangesToRevert, List<PURCommand<Model>> newChanges) {
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
	
//	private final List<Location> getLocationsFromInnerToOuter(Model inner, Model outer, Location<T> locationOfInner) {
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

	@SuppressWarnings("unchecked")
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof PushLocalChanges && sender == source) {
			System.out.println("***Forwarding from " + source + " to " + target + "***");
			
			// Whenever a change is forwarded from a source
			final PushLocalChanges pushLocalChanges = (PushLocalChanges)change;
			
			Location<Model> forwardedOffset = pushLocalChanges.offset.forForwarding();
			final Model innerMostTarget = (Model)CompositeLocation.getChild(this.target, new ModelRootLocation<Model>(), forwardedOffset);
			
			final ArrayList<PURCommand<Model>> forwardedNewChanges = new ArrayList<PURCommand<Model>>();
			
			// Forward new changes
			for(PURCommand<Model> newChange: pushLocalChanges.newChanges)
				forwardedNewChanges.add((PURCommand<Model>) newChange.forForwarding());

			// Forward changes to revert
			final ArrayList<Tuple2<ExecutionScope<Model>, PURCommand<Model>>> forwardedChangesToRevert = new ArrayList<Tuple2<ExecutionScope<Model>, PURCommand<Model>>>();
			for(Tuple2<ExecutionScope<Model>, PURCommand<Model>> newChange: pushLocalChanges.localChangesToRevert) {
				// TODO: Add forwarded scope and forwarded command
//				forwardedChangesToRevert.add((PURCommand<Model>) newChange.forForwarding());
			}

			collector.startTransaction(innerMostTarget, NullTransactionHandler.class);
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
						collector.startTransaction(currentTarget, NullTransactionHandler.class);
//						collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
//							new UnplayCommand(localChangeCount),
//							new ReplayCommand(localChangeCount)
//						)));
						collector.execute(new TriStatePURCommand<Model>(
							new ReversibleCommandPair<Model>(new UnplayCommand(localChangeCount), new ReplayCommand(localChangeCount)), 
							new ReversibleCommandPair<Model>(new ReplayCommand(localChangeCount), new UnplayCommand(localChangeCount)), 
							new ReversibleCommandPair<Model>(new UnplayCommand(localChangeCount), new ReplayCommand(localChangeCount))
						));
						collector.commitTransaction();
					}
					
					// Start transaction for each pair of scope and purcommand to undo
					// - the transaction should be started for the particular scope
					// The new scope must be used later when redoing
					
					// Revert forwarded changes
					for(Tuple2<ExecutionScope<Model>, PURCommand<Model>> scopeAndCmd: forwardedChangesToRevert) {
						collector.startTransaction(target, new ContinueTransactionHandlerFactory<Model>(scopeAndCmd.value1, scopeAndCmd.value2));
						// Just that one command is to be executed
						collector.execute(scopeAndCmd.value1);
						collector.commitTransaction();
					}
					// At this point, the stack should look as follows: many scope and command pairs in sequence

					// Do the forwarded change without affecting the local changes
					// The forwarded changes must be, each, of type PendingCommandState
					collector.execute(forwardedNewChanges);
					
					// Continuation production should be in reverse order due to being pushed into a stack
					// This, the execution is in reverse order fitting doing a redo
					collector.execute(new ExecuteContinuationsFromScopeCommand<Model>(forwardedChangesToRevert.size()));

					// Unplay from innermost part of target to outermost part of target
					// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
					// Play the local changes forward
					
					// Unplay from innermost part of target to outermost part of target
					// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
					for(int i = modelsFromInnerToOuter.size() - 1; i >= 0; i--) {
						Model currentTarget = modelsFromInnerToOuter.get(i);
						int localChangeCount = modelIndexToLocationHistory.get(i);
						modelIndexToLocationHistory.add(localChangeCount);
						// Play the local changes forward
						collector.startTransaction(currentTarget, (Class<? extends TransactionHandler<Model>>)NullTransactionHandler.class);
						collector.execute(new TriStatePURCommand<Model>(
							new ReversibleCommandPair<Model>(new ReplayCommand(localChangeCount), new UnplayCommand(localChangeCount)), 
							new ReversibleCommandPair<Model>(new UnplayCommand(localChangeCount), new ReplayCommand(localChangeCount)), 
							new ReversibleCommandPair<Model>(new ReplayCommand(localChangeCount), new UnplayCommand(localChangeCount))
						));
						collector.commitTransaction();
					}
					
					
//					PendingCommandFactory.Util.executeSequence(collector, forwardedChangesToRevert, new ExecutionsHandler<Model>() {
//						@Override
//						public void handleExecutions(final List<Execution<Model>> forwardedChangesToRevertPendingUndoablePairs, Collector<Model> collector) {
//							// Do the forwarded change without affecting the local changes
//							// The forwarded changes must be, each, of type PendingCommandState
//							final ArrayList<CommandState<Model>> forwardedNewChangesAsPendings = new ArrayList<CommandState<Model>>();
//							for(CommandState<Model> forwardedNewChange: forwardedNewChanges)
//								forwardedNewChange.appendPendings(forwardedNewChangesAsPendings);
//							
//							PendingCommandFactory.Util.executeSequence(collector, forwardedNewChangesAsPendings, new ExecutionsHandler<Model>() {
//								@Override
//								public void handleExecutions(List<Execution<Model>> forwardedNewChangesPendingUndoablePairs, Collector<Model> collector) {
//									// Play the inherited local changes forwards without affecting the local changes
//									ArrayList<CommandState<Model>> backwardOutput = new ArrayList<CommandState<Model>>();
//									// They may not have been any forwarded changes to revert
//									if(forwardedChangesToRevertPendingUndoablePairs != null) {
//										for(Execution<Model> pup: forwardedChangesToRevertPendingUndoablePairs)
//											backwardOutput.add(pup.undoable);
//									}
//									
//									Collections.reverse(backwardOutput);
//
//									PendingCommandFactory.Util.executeSequence(collector, backwardOutput, new ExecutionsHandler<Model>() {
//										@Override
//										public void handleExecutions(List<Execution<Model>> pendingUndoablePairs, Collector<Model> collector) {
//											// Unplay from innermost part of target to outermost part of target
//											// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
//											for(int i = modelsFromInnerToOuter.size() - 1; i >= 0; i--) {
//												Model currentTarget = modelsFromInnerToOuter.get(i);
//												int localChangeCount = modelIndexToLocationHistory.get(i);
//												modelIndexToLocationHistory.add(localChangeCount);
//												// Play the local changes forward
//												collector.startTransaction(currentTarget, (Class<? extends TransactionHandler<Model>>)NullTransactionHandler.class);
//												collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
//													new ReplayCommand(localChangeCount),
//													new UnplayCommand(localChangeCount)
//												)));
//												collector.commitTransaction();
//											}
//										}
//									});
//								}
//							});
//						}
//					});
				}
			});

			collector.commitTransaction();
			
			// Accumulate local changes to revert
			ArrayList<Tuple2<ExecutionScope<Model>, PURCommand<Model>>> newLocalChangesToRevert = new ArrayList<Tuple2<ExecutionScope<Model>, PURCommand<Model>>>();

			// Accumulate from innermost part of target to outermost part of target
			// The outer parts of target could be probably be reduced to commands which affect inner parts, such as the scale command
			Location<Model> currentLocation = new ModelRootLocation<Model>();
			Model currentModel = innerMostTarget;
			
			while(true) {
				for(CommandState<Model> localChangeBackwards: currentModel.getLocalChangesBackwards()) {
					// TODO: Somehow, offset localChangeBackwards
					// Surround with offset transaction handler?
//					newLocalChangesToRevert.add(localChangeBackwards.offset(currentLocation));
				}
				
				if(currentModel != this.target) {
					currentModel = currentModel.getParent();
					currentLocation = new CompositeLocation<Model>(currentLocation, new ParentLocation());
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
