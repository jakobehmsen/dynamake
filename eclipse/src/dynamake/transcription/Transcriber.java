package dynamake.transcription;

import java.io.IOException;

public interface Transcriber<T> {
	void close() throws IOException;
	T prevalentSystem();
	Connection<T> createConnection(TriggerHandler<T> flushHandler);
}
