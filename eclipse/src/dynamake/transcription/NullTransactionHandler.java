package dynamake.transcription;

import java.util.ArrayList;

import dynamake.models.PropogationContext;

public class NullTransactionHandler<T> implements TransactionHandler<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void startLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void logFor(T reference, ArrayList<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void commitLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void rejectLogFor(T reference, PropogationContext propCtx, int propDistance, Collector<T> collector) { }
}
