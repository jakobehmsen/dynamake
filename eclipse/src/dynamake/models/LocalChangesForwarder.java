package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.CreateAndExecuteFromPropertyCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PlayThenReverseCommand;
import dynamake.commands.RedoCommand;
import dynamake.commands.ReplayCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.SetPropertyToOutputCommand;
import dynamake.commands.UndoCommand;
import dynamake.commands.UnplayCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;

/**
 * Instances each are supposed to forward change made in an source to an target.
 * The relation is not supposed to be one-to-one between source and target; instead
 * target are to support isolated changes which are maintained safely even when changes
 * are forwarded from the source.
 */
public class LocalChangesForwarder extends ObserverAdapter implements Serializable {
	public static class PushLocalChanges {
		public final List<CommandState<Model>> localChangesToRevert;
		public final List<CommandState<Model>> newChanges;

		public PushLocalChanges(List<CommandState<Model>> localChangesToRevert, List<CommandState<Model>> newChanges) {
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
			// Whenever a change is forwarded from a source
			final PushLocalChanges pushLocalChanges = (PushLocalChanges)change;
			
			// On a meta level (i.e. build commands which are not going to be part of the inheretee's local changes)
			collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return target;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					int localChangeCount = target.getLocalChangeCount();
					
					// Play the local changes backwards
					commandStates.add(new PendingCommandState<Model>(
						new UnplayCommand(localChangeCount),
						new ReplayCommand(localChangeCount)
					));
					
					// Play the inherited local changes backwards without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyToOutputCommand("backwardOutput", new PlayThenReverseCommand(pushLocalChanges.localChangesToRevert)),
						new PlayThenReverseCommand.AfterPlay()
					));	

					// Do the forwarded change without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new PlayThenReverseCommand(pushLocalChanges.newChanges),
						new PlayThenReverseCommand.AfterPlay()
					));

					// Play the inherited local changes forwards without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyToOutputCommand("forwardOutput", new CreateAndExecuteFromPropertyCommand("backwardOutput", new PlayThenReverseCommand.AfterPlay())),
						new CreateAndExecuteFromPropertyCommand("forwardOutput", new PlayThenReverseCommand.AfterPlay())
					));	

					// Cleanup in properties
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyCommand("forwardOutput", null),
						new SetPropertyCommand.AfterSetProperty()
					));	
					
					// Play the local changes forward
					commandStates.add(new PendingCommandState<Model>(
						new ReplayCommand(localChangeCount),
						new UnplayCommand(localChangeCount)
					));
				}
			});
			
			// Accumulate local changes to revert
			ArrayList<CommandState<Model>> newLocalChangesToRevert = new ArrayList<CommandState<Model>>(pushLocalChanges.localChangesToRevert);
			newLocalChangesToRevert.addAll(target.getLocalChangesBackwards());
			
			target.sendChanged(new PushLocalChanges(newLocalChangesToRevert, pushLocalChanges.newChanges), propCtx, propDistance, changeDistance, collector);
		} else if((change instanceof Model.HistoryAppendLogChange)) {
			if(sender == source) {
				// Forward the logged change in source
				final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
				
				Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
				
				ArrayList<CommandState<Model>> newChanges = new ArrayList<CommandState<Model>>();
				
				if(firstCommandOutput instanceof UndoCommand.Output) {
					newChanges.add(((UndoCommand.Output)firstCommandOutput).command);
				} else if(firstCommandOutput instanceof RedoCommand.Output) {
					newChanges.add(((RedoCommand.Output)firstCommandOutput).command);
				} else {
					for(Model.PendingUndoablePair pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs) {
						CommandState<Model> commandState;
						
						// When a model is added to a canvas, map id to ForwardedId (if not only already ForwardedId)
						// When a model is removed from a canvas, map id to ForwardedId (if not only already ForwardedId)
						if(pendingUndoablePair.pending.getCommand() instanceof CanvasModel.AddModelCommand) {
							CanvasModel.AddModelCommand addCommand = (CanvasModel.AddModelCommand)pendingUndoablePair.pending.getCommand();
							CanvasModel.AddModelCommand.Output addCommandOutput = (CanvasModel.AddModelCommand.Output)pendingUndoablePair.undoable.getOutput();
							
							Location mappedLocation = new CanvasModel.ForwardLocation(addCommandOutput.location);
							CanvasModel.ForwardedAddModelCommand newAddCommand = new CanvasModel.ForwardedAddModelCommand(mappedLocation, addCommand.factory);
							commandState = new PendingCommandState<Model>(newAddCommand, new CanvasModel.RemoveModelCommand.AfterAdd());
						} else
							commandState = pendingUndoablePair.pending;
						newChanges.add(commandState);
					}
				}
				
				this.changed(source, new PushLocalChanges(new ArrayList<CommandState<Model>>(), newChanges), propCtx, propDistance, changeDistance, collector);
			}
		}
	}
}
