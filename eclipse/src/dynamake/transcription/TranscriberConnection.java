package dynamake.transcription;

public interface TranscriberConnection<T> {
	void enqueue(DualCommandFactory<T> transactionFactory);
	void afterNextFlush(Runnable runnable);
	void flush();
	void commit();
	void reject();
}
