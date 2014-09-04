package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.TransactionHandlerFactory;

public class ContinueTransactionHandlerFactory<T> implements TransactionHandlerFactory<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ExecutionScope<T> scope;
	private PURCommand<T> command;

	public ContinueTransactionHandlerFactory(ExecutionScope<T> scope, PURCommand<T> command) {
		this.scope = scope;
		this.command = command;
	}

	@Override
	public TransactionHandler<T> createTransactionHandler(T reference) {
		return new ContinueTransactionHandler<T>(scope, command);
	}
}
