package dynamake.transcription;

public interface Trigger<T> {
	void run(TranscriberCollector<T> collector);
}
