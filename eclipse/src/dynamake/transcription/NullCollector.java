package dynamake.transcription;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void startTransaction(T reference, Object transactionHandlerClass) { }
	
	@Override
	public void produce(Object value) { }
	
	@Override
	public void consume() { }
	
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
