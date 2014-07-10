package dynamake.transcription;

public interface Collector<T> {
	void execute(DualCommandFactory<T> transactionFactory);
	void afterNextTrigger(Runnable runnable);
	void registerAffectedModel(T model);
	void reject();
	void commit();
	
	void pushReference(T model);
	void popReference();
}
