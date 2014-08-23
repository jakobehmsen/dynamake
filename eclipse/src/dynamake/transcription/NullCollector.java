package dynamake.transcription;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void startTransaction(T reference, Class<? extends TransactionHandler<T>> transactionHandlerClass) { }
	
	@Override
	public void execute(Object command) { }

	@Override
	public void commitTransaction() { }

	@Override
	public void rejectTransaction() { }

	@Override
	public void afterNextTrigger(Runnable runnable) { }
	
	@Override
	public void flushNextTrigger() { }
}
