package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.UndoRedoPart;
import dynamake.transcription.Collector;
import dynamake.transcription.Execution;
import dynamake.transcription.TransactionHandler;

public class RedoTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope scope;
	private ArrayList<Execution<Model>> newLog;
	
	public RedoTransactionHandler(ExecutionScope scope) {
		this.scope = scope;
	}
	
	public RedoTransactionHandler() { }

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
//		System.out.println(this +  ": startLogFor");
		newLog = new ArrayList<Execution<Model>>();
	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
//		newLog.addAll(pendingUndoablePairs);
	}

	@Override
	public void commitLogFor(Model reference) {
		// Build undoable from logged commands
		
		@SuppressWarnings("unchecked")
		CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[newLog.size()];

		for(int i = 0; i < newLog.size(); i++) {
			// Unwrap UndoRedoPart
			UndoRedoPart undoRedoPart = (UndoRedoPart)newLog.get(i).undoable;
			compressedLogPartAsArray[i] = undoRedoPart;
		}
		
		RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
		
//		System.out.println(this +  ": commitLogFor");
		reference.commitRedo(compressedLogPart);
	}

	@Override
	public void rejectLogFor(Model reference) {
//		System.out.println(this +  ": rejectLogFor");
	}
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
