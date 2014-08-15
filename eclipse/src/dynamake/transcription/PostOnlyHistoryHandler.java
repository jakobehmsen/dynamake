package dynamake.transcription;

import java.util.ArrayList;

import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class PostOnlyHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}

	@Override
	public void logFor(Model reference, ArrayList<Execution<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.postLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof PostOnlyHistoryHandler;
	}
	
	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}
}
