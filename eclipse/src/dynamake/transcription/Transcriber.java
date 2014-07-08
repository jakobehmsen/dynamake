package dynamake.transcription;

import java.io.IOException;

public interface Transcriber<T> {
	void close() throws IOException;
	T prevalentSystem();
	
	void executeTransient(Runnable runnable);
	
	Connection<T> createConnection(TriggerHandler<T> flushHandler);
}
