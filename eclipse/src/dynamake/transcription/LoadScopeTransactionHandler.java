package dynamake.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.PropogationContext;

public class LoadScopeTransactionHandler<T> implements TransactionHandler<T> {
	private ExecutionScope scope;
	
	public LoadScopeTransactionHandler(ExecutionScope scope) {
		this.scope = scope;
	}
	
	@Override
	public void logBeforeFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector) {
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
	public ExecutionScope getScope() {
		return scope;
	}
}
