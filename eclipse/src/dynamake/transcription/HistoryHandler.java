package dynamake.transcription;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public interface HistoryHandler<T> extends Serializable {
	void startLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void logFor(T reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void commitLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void rejectLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
}
