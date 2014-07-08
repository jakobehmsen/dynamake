package dynamake.transcription;

public interface TranscriberOnFlush<T> {
	void run(TranscriberCollector<T> collector);
}
