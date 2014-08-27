package dynamake.models.transcription;

import dynamake.models.Model;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.TransactionHandlerFactory;

public class UndoTransactionHandlerFactory implements TransactionHandlerFactory<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public TransactionHandler<Model> createTransactionHandler(Model reference) {
		Model.HistoryPart undoPart = reference.getUndoScope();
		
		return new UndoTransactionHandler(undoPart);
	}
}
