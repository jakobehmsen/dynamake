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
	public class Execution<T> implements Serializable, CommandState<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
//		public final PendingCommandState<Model> pending;
		public final CommandState<T> pending;
		public final CommandStateWithOutput<T> undoable;
		
		public Execution(CommandState<T> pending, CommandStateWithOutput<T> undoable) {
			if(pending == null)
				new String();
			this.pending = pending;
			this.undoable = undoable;
		}

		@Override
		public Execution<T> forForwarding() {
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
			
			CommandState<T> newPending = pending.forForwarding(undoable.getOutput());
//			if(undoable.getOutput() instanceof ForwardableOutput)
//				newOutput = ((ForwardableOutput)undoable.getOutput()).forForwarding();
//			else
//				newOutput = undoable.getOutput();
//			ReversibleCommand<Model> newUndoable = new ReversibleCommand<Model>(undoable.getCause(), newOutput, undoable.getForthFactory(), undoable.getBackFactory());
			CommandStateWithOutput<T> newUndoable = (CommandStateWithOutput<T>)undoable.forForwarding();
			return new Execution<T>(newPending, newUndoable);
		}

		@Override
		public Execution<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
			return new Execution<T>(
				(PendingCommandState<T>)pending.mapToReferenceLocation(sourceReference, targetReference),
				(CommandStateWithOutput<T>)undoable.mapToReferenceLocation(sourceReference, targetReference)
			);
		}

		@Override
		public Execution<T> offset(Location offset) {
			return new Execution<T>(
				(PendingCommandState<T>)pending.offset(offset),
				(CommandStateWithOutput<T>)undoable.offset(offset)
			);
		}
		
		@Override
		public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
			return pending.executeOn(propCtx, prevalentSystem, collector, location);
		}
		
		@Override
		public void appendPendings(List<CommandState<T>> pendingCommands) {
			pending.appendPendings(pendingCommands);
			// Assumed, reversible doesn't contain pending commands, and if it does, those commands are insignificant
		}

		@Override
		public CommandState<T> forForwarding(Object output) {
			return null;
		}
	}