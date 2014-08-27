package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

public class UndoTransactionHandler implements TransactionHandler<Model> {
	private Model.HistoryPart undoPart;
//	private ArrayList<Execution<Model>> newLog;
	
	public UndoTransactionHandler(Model.HistoryPart undoPart) {
		this.undoPart = undoPart;
	}
	
	public UndoTransactionHandler() { }

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
//		System.out.println(this +  ": startLogFor");
//		newLog = new ArrayList<Execution<Model>>();
	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
//		newLog.addAll(pendingUndoablePairs);
	}

	@Override
	public void commitLogFor(Model reference) {
//		// Build redoable from logged commands
//		
//		@SuppressWarnings("unchecked")
//		CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[newLog.size()];
//
//		for(int i = 0; i < newLog.size(); i++) {
//			// Unwrap UndoRedoPart
//			UndoRedoPart undoRedoPart = (UndoRedoPart)newLog.get(i).undoable;
//			compressedLogPartAsArray[i] = undoRedoPart;
//		}
//		
//		RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
//		
////		System.out.println(this +  ": commitLogFor");
//		reference.commitUndo(compressedLogPart);
		
		reference.commitUndo(undoPart);
	}

	@Override
	public void rejectLogFor(Model reference) {
//		System.out.println(this +  ": rejectLogFor");
	}
	
	@Override
	public ExecutionScope getScope() {
		return undoPart.getScope();
	}
}
