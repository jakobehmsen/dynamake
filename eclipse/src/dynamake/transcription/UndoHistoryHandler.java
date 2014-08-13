package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.Model.UndoRedoPart;

public class UndoHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<PendingUndoablePair> newLog;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": startLogFor");
		newLog = new ArrayList<Model.PendingUndoablePair>();
	}

	@Override
	public void logFor(Model reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
		newLog.addAll(pendingUndoablePairs);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// Build redoable from logged commands
		
		@SuppressWarnings("unchecked")
		CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[newLog.size()];

		for(int i = 0; i < newLog.size(); i++) {
			// Unwrap UndoRedoPart
//			UndoRedoPart undoRedoPart = (UndoRedoPart)newLog.get(i).pending;
			UndoRedoPart undoRedoPart = (UndoRedoPart)newLog.get(i).undoable;
//			compressedLogPartAsArray[i] = undoRedoPart;//new UndoRedoPart(newLog.get(i), newLog.get(i).undoable);
//			compressedLogPartAsArray[i] = new UndoRedoPart(undoRedoPart.origin, undoRedoPart.origin.undoable);
			compressedLogPartAsArray[i] = undoRedoPart;
		}
		
		RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
		
//		System.out.println(this +  ": commitLogFor");
		reference.commitUndo(compressedLogPart);
	}

	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": rejectLogFor");
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof UndoHistoryHandler;
	}
}
