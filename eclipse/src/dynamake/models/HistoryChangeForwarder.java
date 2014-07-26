package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import dynamake.commands.AppendToListCommand2;
import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.PlayCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.RemovedFromListCommand2;
import dynamake.commands.ReplayCommand2;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UndoCommand;
import dynamake.commands.UnplayCommand;
import dynamake.commands.UnplayCommand2;
import dynamake.models.CanvasModel.AddModelCommand;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;
import dynamake.transcription.TranscribeOnlyPendingCommandFactory;

/**
 * Instances each are supposed to forward change made in an inhereter to an inheretee.
 * The relation is not supposed to be one-to-one between inhereter and inheretee; instead
 * inheretee are to support isolated changes which are maintained safely even when changes
 * are forwarded from the inhereter.
 */
public class HistoryChangeForwarder extends ObserverAdapter implements Serializable {
	public static class ForwardLogChange {
		public final List<Model.DualCommand> newChanges;

		public ForwardLogChange(List<Model.DualCommand> localChanges) {
			this.newChanges = localChanges;
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
					// Play the local changes backwards
					commandStates.add(new PendingCommandState<Model>(
						new UnplayCommand2(),
						new ReplayCommand2()
					));

					// Do the forwarded change without affecting the local changes
					commandStates.add(new PendingCommandState<Model>(
						new PlayForwardCommand2(forwardLogChange.newChanges),
						new PlayBackwardCommand2(forwardLogChange.newChanges)
					));	
					
					// Remember the forwarded change in inheretee
					commandStates.add(new PendingCommandState<Model>(
						new AppendToListCommand2<Model.DualCommand>("Inhereted", forwardLogChange.newChanges),
						new RemovedFromListCommand2.AfterAppendToList<Model.DualCommand>()
					));
					
					// Play the local changes forward
					commandStates.add(new PendingCommandState<Model>(
						new ReplayCommand2(),
						new UnplayCommand2()
					));
				}
			});
			
			inheretee.sendChanged(forwardLogChange, propCtx, propDistance, changeDistance, collector);
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
				
				this.changed(inhereter, new ForwardLogChange(newChanges), propCtx, propDistance, changeDistance, collector);
				
			}
		}
	}
}
