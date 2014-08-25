package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.models.PropogationContext;

public interface TransactionHandler<T> {
	void startLogFor(T reference);
	// Side-effects are valid here. Thus, collector parameter is needed to support this.
	void logFor(T reference, ArrayList<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void commitLogFor(T reference);
	void rejectLogFor(T reference);
	ExecutionScope getScope();
}
