package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.AppendToListCommand;
import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.CreateAndExecuteFromPropertyCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PlayBackwardCommand;
import dynamake.commands.PlayCommand;
import dynamake.commands.PlayForwardCommand2;
import dynamake.commands.RedoCommand;
import dynamake.commands.RemovedFromListCommand;
import dynamake.commands.ReplayCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.SetPropertyToOutputCommand;
import dynamake.commands.UndoCommand;
import dynamake.commands.UnplayCommand;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;

/**
 * Instances each are supposed to forward change made in an inhereter to an inheretee.
 * The relation is not supposed to be one-to-one between inhereter and inheretee; instead
 * inheretee are to support isolated changes which are maintained safely even when changes
 * are forwarded from the inhereter.
 */
public class HistoryChangeForwarder extends ObserverAdapter implements Serializable {
	public static class ForwardLogChange {
		public final List<CommandState<Model>> inheretedLocalChangesBackwards;
		public final List<CommandState<Model>> newChanges;

		public ForwardLogChange(List<CommandState<Model>> inheretedLocalChangesBackwards, List<CommandState<Model>> newChanges) {
			this.inheretedLocalChangesBackwards = inheretedLocalChangesBackwards;
			this.newChanges = newChanges;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model inhereter;
	private Model inheretee;
	
	public HistoryChangeForwarder(Model inhereter, Model inheretee) {
		this.inhereter = inhereter;
		// at this point, inheretee is assumed to be clone of inhereter with no local changes
		this.inheretee = inheretee;
	}
	
	public void attach(PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}
	
	public boolean forwardsTo(Model model) {
		return inheretee == model;
	}
	
	public boolean forwardsFrom(Model model) {
		return inhereter == model;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof HistoryChangeForwarder) {
			HistoryChangeForwarder historyChangeForwarder = (HistoryChangeForwarder)obj;
			return this.inhereter == historyChangeForwarder.inhereter && this.inheretee == historyChangeForwarder.inheretee;
		}
		return false;
	}

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof ForwardLogChange && sender == inhereter) {
			// Whenever a change is forwarded from an inhereter
			final ForwardLogChange forwardLogChange = (ForwardLogChange)change;
			
			// On a meta level (i.e. build commands which are not going to be part of the inheretee's local changes)
			collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return inheretee;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					int localChangeCount = inheretee.getLocalChangeCount();
					
					// Play the local changes backwards
					commandStates.add(new PendingCommandState<Model>(
						new UnplayCommand(localChangeCount),
						new ReplayCommand(localChangeCount)
					));

//					// Play the inherited local changes backwards without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new PlayBackwardCommand2(forwardLogChange.inheretedLocalChanges),
//						new PlayForwardCommand2(forwardLogChange.inheretedLocalChanges)
//					));	
//
//					// Do the forwarded change without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new PlayForwardCommand2(forwardLogChange.newChanges),
//						new PlayBackwardCommand2(forwardLogChange.newChanges)
//					));	
//					
//					// Remember the forwarded change in inheretee
//					commandStates.add(new PendingCommandState<Model>(
//						new AppendToListCommand2<Model.DualCommand>("Inhereted", forwardLogChange.newChanges),
//						new RemovedFromListCommand2.AfterAppendToList<Model.DualCommand>()
//					));
//
//					// Play the inherited local changes forwards without affecting the local changes
//					commandStates.add(new PendingCommandState<Model>(
//						new PlayForwardCommand2(forwardLogChange.inheretedLocalChanges),
//						new PlayBackwardCommand2(forwardLogChange.inheretedLocalChanges)
//					));	
					
					// Play the inherited local changes backwards without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyToOutputCommand("backwardOutput", new PlayCommand(forwardLogChange.inheretedLocalChangesBackwards)),
						new PlayCommand.AfterPlay()
					));	

					// Do the forwarded change without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new PlayCommand(forwardLogChange.newChanges),
						new PlayCommand.AfterPlay()
					));	
					
					// Remember the forwarded change in inheretee
					commandStates.add(new PendingCommandState<Model>(
						new AppendToListCommand<CommandState<Model>>("Inhereted", forwardLogChange.newChanges),
						new RemovedFromListCommand.AfterAppendToList<Model.DualCommand>()
					));

					// Play the inherited local changes forwards without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyToOutputCommand("forwardOutput", new CreateAndExecuteFromPropertyCommand("backwardOutput", new PlayCommand.AfterPlay())),
						new CreateAndExecuteFromPropertyCommand("forwardOutput", new PlayCommand.AfterPlay())
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
			
			ArrayList<CommandState<Model>> newInheritedLocalChangesBackwards = new ArrayList<CommandState<Model>>(forwardLogChange.inheretedLocalChangesBackwards);
//			List<Model.DualCommand> inheritedChanges = (List<Model.DualCommand>)inhereter.getProperty("Inhereted");
//			if(inheritedChanges != null)
//				newInheritedLocalChanges.addAll(inheritedChanges);
			newInheritedLocalChangesBackwards.addAll(inheretee.getLocalChangesBackwards());
			
			inheretee.sendChanged(new ForwardLogChange(newInheritedLocalChangesBackwards, forwardLogChange.newChanges), propCtx, propDistance, changeDistance, collector);
		} else if((change instanceof Model.HistoryAppendLogChange)) {
			if(sender == inhereter) {
				// Forward the logged change in inhereter
				final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
				
				Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
				
				ArrayList<CommandState<Model>> newChanges = new ArrayList<CommandState<Model>>();
				
				if(firstCommandOutput instanceof UndoCommand.Output) {
					newChanges.add(((UndoCommand.Output)firstCommandOutput).command);
				} else if(firstCommandOutput instanceof RedoCommand.Output) {
					newChanges.add(((RedoCommand.Output)firstCommandOutput).command);
				} else {
					for(Model.PendingUndoablePair pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs) {
//						// Map each command state to its forward compatible correspondant
//						if(pendingUndoablePair.pending.getCommand() instanceof CanvasModel.RestoreModelCommand) {
//							CanvasModel.RestoreModelCommand restoreModelCommand = (CanvasModel.RestoreModelCommand)pendingUndoablePair.pending.getCommand();
//							Command<Model> commandClone = restoreModelCommand.cloneCommand();
//							newChanges.add(new PendingCommandState<Model>(commandClone, pendingUndoablePair.pending.getForthFactory(), pendingUndoablePair.pending.getBackFactory()));
//						} else
							newChanges.add(pendingUndoablePair.pending);
					}
				}
				
				this.changed(inhereter, new ForwardLogChange(new ArrayList<CommandState<Model>>(), newChanges), propCtx, propDistance, changeDistance, collector);
				
			}
		}
	}
}
