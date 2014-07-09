package dynamake.transcription;

public interface Collector<T> {
	void execute(DualCommandFactory<T> transactionFactory);
	void afterNextTrigger(Runnable runnable);
	void registerAffectedModel(T model);
	void enlistReject();
	void enlistCommit();
}
