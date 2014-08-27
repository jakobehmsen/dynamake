package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.models.Model;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.TransactionHandlerFactory;

public class RedoTransactionHandlerFactory implements TransactionHandlerFactory<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public TransactionHandler<Model> createTransactionHandler(Model reference) {
		ExecutionScope scope = reference.getRedoScope();
		
		return new RedoTransactionHandler(scope);
	}
}
