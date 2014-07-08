package dynamake.transcription;

public interface TranscriberConnection<T> {
	void trigger(Trigger<T> trigger);
}
