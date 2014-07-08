package dynamake.transcription;

import java.util.List;

public interface TriggerHandler<T> {
	void handleAfterTrigger(List<Runnable> runnables);
}
