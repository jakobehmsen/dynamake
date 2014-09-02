package dynamake.models;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.models.transcription.ContinueTransactionHandler;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.TransactionHandlerFactory;

public class ContinueTransactionHandlerFactory implements TransactionHandlerFactory<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ExecutionScope<Model> scope;
	private PURCommand<Model> command;

	public ContinueTransactionHandlerFactory(ExecutionScope<Model> scope, PURCommand<Model> command) {
		this.scope = scope;
		this.command = command;
	}

	@Override
	public TransactionHandler<Model> createTransactionHandler(Model reference) {
		return new ContinueTransactionHandler(scope, command);
	}
}
