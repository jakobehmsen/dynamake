package dynamake.transcription;

public interface TranscriberConnection<T> {
	void trigger(TranscriberRunnable<T> runnable);
}
