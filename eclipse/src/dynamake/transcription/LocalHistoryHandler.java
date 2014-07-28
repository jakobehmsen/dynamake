package dynamake.transcription;

import java.util.ArrayList;

import dynamake.models.Model;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.PropogationContext;

public class LocalHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}

	@Override
	public void logFor(Model reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.appendLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.commitLog(propCtx, propDistance, collector);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof LocalHistoryHandler;
	}

	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.rejectLog(propCtx, propDistance, collector);
	}
}
