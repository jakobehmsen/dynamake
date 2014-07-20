package dynamake.transcription;

/**
 * Instances of implementors represents triggers that indicate side effects on a collector.
 */
public interface Trigger<T> {
	void run(Collector<T> collector);
}
