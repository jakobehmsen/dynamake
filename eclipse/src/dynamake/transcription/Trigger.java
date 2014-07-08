package dynamake.transcription;

public interface Trigger<T> {
	void run(Collector<T> collector);
}
