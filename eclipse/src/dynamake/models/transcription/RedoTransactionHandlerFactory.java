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
		Model.HistoryPart redoPart = reference.getRedoScope();
		
		return new RedoTransactionHandler(redoPart);
	}
}
