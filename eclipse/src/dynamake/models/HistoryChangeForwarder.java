package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dynamake.commands.AppendToListCommand;
import dynamake.commands.CommandState;
import dynamake.commands.CreateAndExecuteFromPropertyCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.RemovedFromListCommand;
import dynamake.commands.ReplayCommand;
import dynamake.commands.SetPropertyCommand;
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
		public final List<Model.DualCommand> inheretedLocalChanges;
		public final List<Model.DualCommand> newChanges;

		public ForwardLogChange(List<Model.DualCommand> inheretedLocalChanges, List<Model.DualCommand> newChanges) {
			this.inheretedLocalChanges = inheretedLocalChanges;
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
						new SetPropertyToOutputCommand("backwardOutput", new PlayBackwardCommand2(forwardLogChange.inheretedLocalChanges)),
						new PlayForwardCommand2.AfterPlayBackward()
					));	

					// Do the forwarded change without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new PlayForwardCommand2(forwardLogChange.newChanges),
						new PlayBackwardCommand2(forwardLogChange.newChanges)
					));	
					
					// Remember the forwarded change in inheretee
					commandStates.add(new PendingCommandState<Model>(
						new AppendToListCommand<Model.DualCommand>("Inhereted", forwardLogChange.newChanges),
						new RemovedFromListCommand.AfterAppendToList<Model.DualCommand>()
					));

					// Play the inherited local changes forwards without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyToOutputCommand("forwardOutput", new CreateAndExecuteFromPropertyCommand("backwardOutput", new PlayForwardCommand2.AfterPlayBackward())),
						new CreateAndExecuteFromPropertyCommand("forwardOutput", new PlayBackwardCommand2.AfterPlayForward())
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
			
			ArrayList<Model.DualCommand> newInheritedLocalChanges = new ArrayList<Model.DualCommand>(forwardLogChange.inheretedLocalChanges);
//			List<Model.DualCommand> inheritedChanges = (List<Model.DualCommand>)inhereter.getProperty("Inhereted");
//			if(inheritedChanges != null)
//				newInheritedLocalChanges.addAll(inheritedChanges);
			newInheritedLocalChanges.addAll(inheretee.getLocalChanges());
			
			inheretee.sendChanged(new ForwardLogChange(newInheritedLocalChanges, forwardLogChange.newChanges), propCtx, propDistance, changeDistance, collector);
		} else if((change instanceof Model.HistoryAppendLogChange)) {
			if(sender == inhereter) {
				// Forward the logged change in inhereter
				final Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
				
				Object firstCommandOutput = historyAppendLogChange.pendingUndoablePairs.get(0).undoable.getOutput();
				
				ArrayList<Model.DualCommand> newChanges = new ArrayList<Model.DualCommand>();
				
				if(firstCommandOutput instanceof UndoCommand.Output) {
					Model.DualCommand pair = ((UndoCommand.Output)firstCommandOutput).dualCommand;
					newChanges.add(pair);
				} else if(firstCommandOutput instanceof RedoCommand.Output) {
					Model.DualCommand pair = ((RedoCommand.Output)firstCommandOutput).dualCommand;
					newChanges.add(pair);
				} else {
					for(Model.PendingUndoablePair pendingUndoablePair: historyAppendLogChange.pendingUndoablePairs) {
						newChanges.add(new Model.DualCommand(pendingUndoablePair.pending, pendingUndoablePair.undoable));
					}
				}
				
				this.changed(inhereter, new ForwardLogChange(new ArrayList<Model.DualCommand>(), newChanges), propCtx, propDistance, changeDistance, collector);
				
			}
		}
	}
}
