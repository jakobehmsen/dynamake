package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.UndoRedoPart;

public class NewChangeTransactionHandler implements TransactionHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Execution<Model>> newLog;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance) {
		newLog = new ArrayList<Execution<Model>>();
	}

	@Override
	public void logFor(Model reference, ArrayList<Execution<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		newLog.addAll(pendingUndoablePairs);
		reference.appendLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance) {
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
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance) {

	}
}
