package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.ExecutionScope;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.UndoRedoPart;
import dynamake.transcription.Collector;
import dynamake.transcription.Execution;
import dynamake.transcription.TransactionHandler;

public class NewChangeTransactionHandler implements TransactionHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExecutionScope scope;
	private ArrayList<Execution<Model>> newLog;

	@Override
	public void startLogFor(Model reference) {
		scope = new ExecutionScope();
		newLog = new ArrayList<Execution<Model>>();
	}

	@Override
	public void logFor(Model reference, ArrayList<Execution<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		newLog.addAll(pendingUndoablePairs);
		reference.appendLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference) {
		if(newLog.size() > 0) {
			@SuppressWarnings("unchecked")
			CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[newLog.size()];

			for(int i = 0; i < newLog.size(); i++) {
				compressedLogPartAsArray[i] = new UndoRedoPart(newLog.get(i), newLog.get(i).undoable);
			}
			
			RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
			reference.commitLog(compressedLogPart);
		}
	}

	@Override
	public void rejectLogFor(Model reference) {

	}
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
