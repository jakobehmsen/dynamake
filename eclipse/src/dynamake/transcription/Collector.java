package dynamake.transcription;

/**
 * Instances of implementors provided methods to indicate and manage different kinds of side effects.
 */
public interface Collector<T> {
	void execute(Object command);
	void afterNextTrigger(Runnable runnable);
	void reject();
	void commit();
	void flushNextTrigger();
}
