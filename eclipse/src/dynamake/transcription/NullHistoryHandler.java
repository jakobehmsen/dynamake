package dynamake.transcription;

import java.util.ArrayList;

import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.PropogationContext;

public class NullHistoryHandler<T> implements HistoryHandler<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void startLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void logFor(T reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void commitLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void rejectLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NullHistoryHandler;
	}
}
