package dynamake.transcription;


import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.PropogationContext;

public class NullTransactionHandler<T> implements TransactionHandler<T> {
	private ExecutionScope scope;

	@Override
	public void startLogFor(TransactionHandler<T> parentHandler, T reference) { 
		scope = new ExecutionScope();
	}
	
	@Override
	public void logBeforeFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void logFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void commitLogFor(T reference) { }

	@Override
	public void rejectLogFor(T reference) { }
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
