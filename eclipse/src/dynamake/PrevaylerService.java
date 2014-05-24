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
	
	/**
	These three methods should be replaced by a single method: createTransaction (or createConnection)
	Instances of this kind of object (Transaction or connection) is used to push forward dual command
	which are executed at some point in time (for the moment, it should just be immediately)
	
	To finish usage of a connection, either commit or rollback is invoked.
	
	Connection should replace usages of the direct execute method in the future.
	 */
	void beginTransaction();
	void commitTransaction(PropogationContext propCtx);
	void rollbackTransaction(PropogationContext propCtx);
	
	PrevaylerServiceConnection<T> createConnection();
	
	void executeTransient(Runnable runnable);
}
