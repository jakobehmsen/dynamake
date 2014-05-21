package dynamake;

import java.io.IOException;

import org.prevayler.Transaction;

public interface PrevaylerService<T> {
//	void execute(Transaction<T> transaction);
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void close() throws IOException;
	T prevalentSystem();
	void undo(PropogationContext propCtx);
	void redo(PropogationContext propCtx);
	void beginTransaction();
	void commitTransaction(PropogationContext propCtx);
	void rollbackTransaction(PropogationContext propCtx);
	void executeTransient(Runnable runnable);
}
