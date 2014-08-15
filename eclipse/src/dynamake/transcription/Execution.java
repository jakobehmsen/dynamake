package dynamake.transcription;

import java.io.Serializable;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.CommandStateWithOutput;
import dynamake.commands.PendingCommandState;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

// TODO: 
	// - Move class out of model
	// - Put type parameter to used for pending and undoable
	public class Execution implements Serializable, CommandState<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
//		public final PendingCommandState<Model> pending;
		public final CommandState<Model> pending;
		public final CommandStateWithOutput<Model> undoable;
		
		public Execution(CommandState<Model> pending, CommandStateWithOutput<Model> undoable) {
			if(pending == null)
				new String();
			this.pending = pending;
			this.undoable = undoable;
		}

		@Override
		public Execution forForwarding() {
//			if(pending.getCommand() instanceof ForwardableCommand) {
//				Command<Model> commandForForwarding = ((ForwardableCommand<Model>)pending.getCommand()).forForwarding(undoable.getOutput());
//				Object newOutput;
//				if(undoable.getOutput() instanceof ForwardableOutput)
//					newOutput = ((ForwardableOutput)undoable.getOutput()).forForwarding();
//				else
//					newOutput = undoable.getOutput();	
//				// Should cause, forthFactory, and backFactory also be forwarded of undoable?
//				ReversibleCommand<Model> newUndoable = new ReversibleCommand<Model>(undoable.getCause(), newOutput, undoable.getForthFactory(), undoable.getBackFactory());
//				return new PendingUndoablePair(new PendingCommandState<Model>(commandForForwarding, pending.getForthFactory(), pending.getBackFactory()), newUndoable);
//			}
//			
//			return this;
			
			CommandState<Model> newPending = pending.forForwarding(undoable.getOutput());
//			if(undoable.getOutput() instanceof ForwardableOutput)
//				newOutput = ((ForwardableOutput)undoable.getOutput()).forForwarding();
//			else
//				newOutput = undoable.getOutput();
//			ReversibleCommand<Model> newUndoable = new ReversibleCommand<Model>(undoable.getCause(), newOutput, undoable.getForthFactory(), undoable.getBackFactory());
			CommandStateWithOutput<Model> newUndoable = (CommandStateWithOutput<Model>)undoable.forForwarding();
			return new Execution(newPending, newUndoable);
		}

		@Override
		public Execution mapToReferenceLocation(Model sourceReference, Model targetReference) {
			return new Execution(
				(PendingCommandState<Model>)pending.mapToReferenceLocation(sourceReference, targetReference),
				(CommandStateWithOutput<Model>)undoable.mapToReferenceLocation(sourceReference, targetReference)
			);
		}

		@Override
		public Execution offset(Location offset) {
			return new Execution(
				(PendingCommandState<Model>)pending.offset(offset),
				(CommandStateWithOutput<Model>)undoable.offset(offset)
			);
		}
		
		@Override
		public CommandState<Model> executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			return pending.executeOn(propCtx, prevalentSystem, collector, location);
		}
		
		@Override
		public void appendPendings(List<CommandState<Model>> pendingCommands) {
			pending.appendPendings(pendingCommands);
			// Assumed, reversible doesn't contain pending commands, and if it does, those commands are insignificant
		}

		@Override
		public CommandState<Model> forForwarding(Object output) {
			return null;
		}
	}