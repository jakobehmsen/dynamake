package dynamake.transcription;

public interface TranscriberCollector<T> {
	void enlist(DualCommandFactory<T> transactionFactory);
	void execute(DualCommandFactory<T> transactionFactory);
	void afterNextTrigger(Runnable runnable);
	void registerAffectedModel(T model);
	void enlistReject();
	void enlistCommit();
	void flush();
}
