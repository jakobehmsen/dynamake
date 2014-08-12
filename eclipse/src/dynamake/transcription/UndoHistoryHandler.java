package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.models.Model;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public class UndoHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ArrayList<PendingUndoablePair> newLog;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		System.out.println(this +  ": startLogFor");
		newLog = new ArrayList<Model.PendingUndoablePair>();
	}

	@Override
	public void logFor(Model reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		System.out.println(this +  ": logFor");
		newLog.addAll(pendingUndoablePairs);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// Build redoable from logged commands
		
//		CommandState<Model> redoable = toUndo.executeOn(propCtx, this, collector, new ModelRootLocation());
//		redoStack.push(redoable);
		
		System.out.println(this +  ": commitLogFor");
	}

	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		System.out.println(this +  ": rejectLogFor");
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof UndoHistoryHandler;
	}
}
