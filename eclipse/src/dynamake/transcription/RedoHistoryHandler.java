package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.UndoRedoPart;

public class RedoHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<Execution<Model>> newLog;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": startLogFor");
		newLog = new ArrayList<Execution<Model>>();
	}

	@Override
	public void logFor(Model reference, ArrayList<Execution<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
		newLog.addAll(pendingUndoablePairs);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
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
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": rejectLogFor");
	}
}
