package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.models.PropogationContext;

public class NullTransactionHandler<T> implements TransactionHandler<T> {
	private ExecutionScope scope;

	@Override
	public void startLogFor(T reference) { 
		scope = new ExecutionScope();
	}

	@Override
	public void logFor(T reference, ArrayList<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void commitLogFor(T reference) { }

	@Override
	public void rejectLogFor(T reference) { }
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
