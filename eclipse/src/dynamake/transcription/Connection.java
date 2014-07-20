package dynamake.transcription;

/**
 * Instances of implementors are supposed to be able to perform triggers on which collectors can indicate certain side effects.
 */
public interface Connection<T> {
	void trigger(Trigger<T> trigger);
}
