package dynamake.transcription;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void execute(DualCommandFactory<T> transactionFactory) { }

	@Override
	public void afterNextTrigger(Runnable runnable) { }

	@Override
	public void registerAffectedModel(T model) { }

	@Override
	public void reject() { }

	@Override
	public void commit() { }

	@Override
	public void pushReference(T model) { }

	@Override
	public void popReference() { }
}
