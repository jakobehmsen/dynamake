package dynamake.transcription;

import java.util.List;

import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public abstract class SimpleExPendingCommandFactory2<T> implements ExPendingCommandFactory2<T> {
	@Override
	public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
	
	@Override
	public HistoryHandler<T> getHistoryHandler() {
		return new NullHistoryHandler<T>();
	}
}
