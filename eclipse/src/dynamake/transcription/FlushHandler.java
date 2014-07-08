package dynamake.transcription;

import java.util.List;

public interface FlushHandler<T> {
	void handleFlush(List<Runnable> runnables);
}
