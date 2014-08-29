package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ForthPURCommand;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

public class NewChangeTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope scope;
	private ArrayList<ReversibleCommand<Model>> newLog;

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
		scope = new ExecutionScope();
		newLog = new ArrayList<ReversibleCommand<Model>>();
	}
	
	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		newLog.add(command);
		
		ArrayList<ReversibleCommand<Model>> pendingUndoablePairs = new ArrayList<ReversibleCommand<Model>>();
		pendingUndoablePairs.add(command);
		reference.appendLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference) {
		if(newLog.size() > 0) {
//			@SuppressWarnings("unchecked")
//			CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[newLog.size()];
//
//			for(int i = 0; i < newLog.size(); i++) {
//				compressedLogPartAsArray[i] = new UndoRedoPart(newLog.get(i), newLog.get(i).undoable);
//			}
//			
//			RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);

//			ArrayList<PURCommand<Model>> undoables = new ArrayList<PURCommand<Model>>();
//			for(ReversibleCommand<Model> pur: newLog) {
//				// pur.back is assumed to be insignificant
//				PURCommand<Model> undoable = ((PURCommand<Model>)pur.forth).inUndoState();
//				undoables.add(undoable);
//			}
//			Collections.reverse(undoables);
//			CommandSequence<Model> compressedLogPart = new CommandSequence<Model>(undoables);
			
			ArrayList<PURCommand<Model>> newPurs = new ArrayList<PURCommand<Model>>();
			for(ReversibleCommand<Model> rc: newLog) {
				PURCommand<Model> pur;
				if(rc instanceof PURCommand)
					pur = (PURCommand<Model>)rc;
				else
					pur = new ForthPURCommand<Model>(rc);
				newPurs.add(pur);
			}
			reference.commitLog(scope, newPurs);
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
