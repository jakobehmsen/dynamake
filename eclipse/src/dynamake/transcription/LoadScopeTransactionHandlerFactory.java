package dynamake.transcription;

import dynamake.commands.ExecutionScope;

public class LoadScopeTransactionHandlerFactory<T> implements TransactionHandlerFactory<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExecutionScope<T> scope;

	public LoadScopeTransactionHandlerFactory(ExecutionScope<T> scope) {
		this.scope = scope;
	}

	@Override
	public TransactionHandler<T> createTransactionHandler(T reference) {
		return new LoadScopeTransactionHandler<T>(scope);
	}
}
