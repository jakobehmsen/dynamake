package dynamake.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.PropogationContext;

public class LoadScopeTransactionHandler<T> implements TransactionHandler<T> {
	private ExecutionScope<T> scope;
	
	public LoadScopeTransactionHandler(ExecutionScope<T> scope) {
		this.scope = scope;
	}
	
	@Override
	public void logBeforeFor(T reference, Object command, PropogationContext propCtx, int propDistance, Collector<T> collector) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startLogFor(TransactionHandler<T> parentHandler, T reference) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commitLogFor(T reference) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectLogFor(T reference) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExecutionScope<T> getScope() {
		return scope;
	}
}
