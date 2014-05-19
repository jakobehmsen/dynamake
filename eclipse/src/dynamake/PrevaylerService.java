package dynamake;

import java.io.IOException;

import org.prevayler.Transaction;

public interface PrevaylerService<T> {
//	void execute(Transaction<T> transaction);
	void execute(PropogationContext propCtx, DualCommand<T> transaction);
	void close() throws IOException;
	T prevalentSystem();
	void undo(PropogationContext propCtx);
	void redo(PropogationContext propCtx);
	void beginTransaction();
	void commitTransaction(PropogationContext propCtx);
	void rollbackTransaction(PropogationContext propCtx);
}
