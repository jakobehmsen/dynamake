package dynamake.transcription;

import java.io.Serializable;

public interface TransactionHandlerFactory<T> extends Serializable {
	TransactionHandler<T> createTransactionHandler();
}
