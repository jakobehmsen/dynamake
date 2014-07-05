package dynamake.transcription;

public interface TranscriberConnection<T> {
	void enqueue(DualCommandFactory<T> transactionFactory);
	void afterNextFlush(TranscriberRunnable<T> runnable);
	void flush();
	void commit();
	void reject();
}
