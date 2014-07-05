package dynamake.transcription;

public interface TranscriberCollector<T> {
	void enqueue(DualCommandFactory<T> transactionFactory);
	void afterNextFlush(TranscriberRunnable<T> runnable);
	void registerAffectedModel(T model);
	void beginIsolation();
	void endIsolation();
}
