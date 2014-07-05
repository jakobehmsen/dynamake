package dynamake.transcription;

public interface TranscriberRunnable<T> {
	void run(TranscriberCollector<T> collector);
}
