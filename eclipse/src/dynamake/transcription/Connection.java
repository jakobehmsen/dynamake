package dynamake.transcription;

public interface Connection<T> {
	void trigger(Trigger<T> trigger);
}
