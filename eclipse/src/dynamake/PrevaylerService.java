package dynamake;

import java.io.IOException;

import org.prevayler.Transaction;

public interface PrevaylerService<T> {
	void execute(Transaction<T> transaction);
	void close() throws IOException;
}
