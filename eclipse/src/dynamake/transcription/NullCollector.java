package dynamake.transcription;

public class NullCollector<T> implements TranscriberCollector<T> {
	@Override
	public void enlist(DualCommandFactory<T> transactionFactory) { }
	
	@Override
	public void execute(DualCommandFactory<T> transactionFactory) { }

	@Override
	public void afterNextTrigger(TranscriberOnFlush<T> runnable) { }

	@Override
	public void registerAffectedModel(T model) { }

	@Override
	public void enlistReject() { }

	@Override
	public void enlistCommit() { }
	
	@Override
	public void flush() { }
}
