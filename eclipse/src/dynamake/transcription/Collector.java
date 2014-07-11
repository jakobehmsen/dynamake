package dynamake.transcription;

public interface Collector<T> {
	void execute(Object command);
	void afterNextTrigger(Runnable runnable);
	void registerAffectedModel(T model);
	void reject();
	void commit();
	void flushNextTrigger();
}
