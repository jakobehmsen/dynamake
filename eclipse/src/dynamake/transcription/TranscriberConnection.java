package dynamake.transcription;

public interface TranscriberConnection<T> {
//	void enqueue(DualCommandFactory<T> transactionFactory);
	void trigger(TranscriberRunnable<T> runnable);
//	void flush();
}
