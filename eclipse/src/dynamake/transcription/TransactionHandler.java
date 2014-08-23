package dynamake.transcription;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.models.PropogationContext;

public interface TransactionHandler<T> extends Serializable {
	void startLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void logFor(T reference, ArrayList<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void commitLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void rejectLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector);
}
