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
import dynamake.transcription.Execution;
import dynamake.transcription.SimpleExPendingCommandFactory;
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

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof PushLocalChanges && sender == source) {
			System.out.println("***Forwarding from " + source + " to " + target + "***");
			
			// Whenever a change is forwarded from a source
			final PushLocalChanges pushLocalChanges = (PushLocalChanges)change;
			
			Location forwardedOffset = pushLocalChanges.offset.forForwarding();
			final Model target = (Model)CompositeLocation.getChild(this.target, new ModelRootLocation(), forwardedOffset);
			
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
					final int localChangeCount = target.getLocalChangeCount();
					
					// Assumed that unplaying doesn't provoke side effects
					// Play the local changes backwards
					collector.execute(new SimpleExPendingCommandFactory<Model>(target, new PendingCommandState<Model>(
						new UnplayCommand(localChangeCount),
						new ReplayCommand(localChangeCount)
					)));

					// Play the inherited local changes backwards without affecting the local changes
					collector.execute(new SimpleExPendingCommandFactory<Model>(target, forwardedChangesToRevert) {
						@Override
						public void afterPropogationFinished(final List<Execution> forwardedChangesToRevertPendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
							// Do the forwarded change without affecting the local changes
							// The forwarded changes must be, each, of type PendingCommandState
							final ArrayList<CommandState<Model>> forwardedNewChangesAsPendings = new ArrayList<CommandState<Model>>();
							for(CommandState<Model> forwardedNewChange: forwardedNewChanges) {
//								// UGLY UGLY HACK(s)!!!
//								if(forwardedNewChange instanceof Model.PendingUndoablePair)
//									forwardedNewChangesAsPendings.add(((Model.PendingUndoablePair)forwardedNewChange).pending);
//								else {// if(forwardedNewChange instanceof RevertingCommandStateSequence) {
//									// This case is provoked in undo scenarios
//									// What to do here?
////									RevertingCommandStateSequence<Model> forwardedNewChangeAsRevertingCommandStateSequence = 
////										(RevertingCommandStateSequence<Model>)forwardedNewChange;
////									new PendingCommandState<Model>(forwardedNewChangeAsRevertingCommandStateSequence, new Command.Null<Model>());
////									forwardedNewChangesAsPendings.add(((Model.PendingUndoablePair)forwardedNewChange).pending);
//									forwardedNewChangesAsPendings.add(forwardedNewChange);
//								}
								
								forwardedNewChange.appendPendings(forwardedNewChangesAsPendings);
								
//								forwardedNewChangesAsPendings.add(forwardedNewChange);
							}
							
							collector.execute(new SimpleExPendingCommandFactory<Model>(target, forwardedNewChangesAsPendings) {
								@Override
								public void afterPropogationFinished(List<Execution> forwardedNewChangesPendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
									// Play the inherited local changes forwards without affecting the local changes
									ArrayList<CommandState<Model>> backwardOutput = new ArrayList<CommandState<Model>>();
									// They may not have been any forwarded changes to revert
									if(forwardedChangesToRevertPendingUndoablePairs != null) {
										for(Execution pup: forwardedChangesToRevertPendingUndoablePairs)
											backwardOutput.add(pup.undoable);
									}
									Collections.reverse(backwardOutput);

									collector.execute(new SimpleExPendingCommandFactory<Model>(target, backwardOutput) {
										@Override
										public void afterPropogationFinished(List<Execution> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
											// Play the local changes forward
											collector.execute(new SimpleExPendingCommandFactory<Model>(target, new PendingCommandState<Model>(
												new ReplayCommand(localChangeCount),
												new UnplayCommand(localChangeCount)
											)));
										}
									});
								}
							});
						}
					});
				}
			});
			
			
			
			
			
//			// On a meta level (i.e. build commands which are not going to be part of the inheretee's local changes)
//			collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
//				@Override
//				public Model getReference() {
//					return target;
//				}
//				
//				@Override
//				public void createPendingCommands(List<CommandState<Model>> commandStates) {
//					// TODO:
//					// Don't use playThenReverse:
//					// - Instead, use collector with SimpleExPendingCommandState and ChainCommand to sync command sequence and retrieve undoables
//					
//					int localChangeCount = target.getLocalChangeCount();
//					
//					// Play the local changes backwards
//					commandStates.add(new PendingCommandState<Model>(
//						new UnplayCommand(localChangeCount),
//						new ReplayCommand(localChangeCount)
//					));
//					
//					// Play the inherited local changes backwards without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new SetPropertyToOutputCommand("backwardOutput", new PlayThenReverseCommand(forwardedChangesToRevert)),
//						new PlayThenReverseCommand.AfterPlay()
//					));	
//
//					// Do the forwarded change without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new PlayThenReverseCommand(forwardedNewChanges),
//						new PlayThenReverseCommand.AfterPlay()
//					));
//
//					// Play the inherited local changes forwards without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new SetPropertyToOutputCommand("forwardOutput", new CreateAndExecuteFromPropertyCommand("backwardOutput", new PlayThenReverseCommand.AfterPlay())),
//						new CreateAndExecuteFromPropertyCommand("forwardOutput", new PlayThenReverseCommand.AfterPlay())
//					));	
//
//					// Cleanup in properties
//					commandStates.add(new PendingCommandState<Model>(
//						new SetPropertyCommand("forwardOutput", null),
//						new SetPropertyCommand.AfterSetProperty()
//					));	
//					
//					// Play the local changes forward
//					commandStates.add(new PendingCommandState<Model>(
//						new ReplayCommand(localChangeCount),
//						new UnplayCommand(localChangeCount)
//					));
//				}
//			});
			
			// Accumulate local changes to revert
			ArrayList<CommandState<Model>> newLocalChangesToRevert = new ArrayList<CommandState<Model>>();
			
			newLocalChangesToRevert.addAll(target.getLocalChangesBackwards());
			
//			// Is there some creation for source? Then this creation should also be (initially) forwarded
//			// Creation must be a list of PendingUndoablePair.
//			@SuppressWarnings("unchecked")
//			List<Model.PendingUndoablePair> sourceCreation = (List<Model.PendingUndoablePair>)source.getProperty("Creation");
//			if(sourceCreation != null) {
//				for(int i = sourceCreation.size() - 1; i >= 0; i--) {
//					Model.PendingUndoablePair sourceCreationPart = sourceCreation.get(i);
//					PendingCommandState<Model> pending = (PendingCommandState<Model>)sourceCreationPart.pending;
//					if(!(pending.getCommand() instanceof ForwardLocalChangesCommand) && !(pending.getCommand() instanceof ForwardLocalChangesUpwardsCommand))
//						newLocalChangesToRevert.add(sourceCreationPart.undoable);
////					if(!(sourceCreationPart.pending.getCommand() instanceof ForwardLocalChangesCommand) && !(sourceCreationPart.pending.getCommand() instanceof ForwardLocalChangesUpwards2Command))
////						newLocalChangesToRevert.add(sourceCreationPart.undoable);
//				}
//			}
			
			newLocalChangesToRevert.addAll(forwardedChangesToRevert);
			
			this.target.sendChanged(new PushLocalChanges(forwardedOffset, newLocalChangesToRevert, forwardedNewChanges), propCtx, propDistance, changeDistance, collector);
		}
	}
}
