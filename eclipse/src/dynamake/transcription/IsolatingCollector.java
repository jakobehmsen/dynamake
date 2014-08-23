package dynamake.transcription;

public class IsolatingCollector<T> implements Collector<T> {
	private Collector<T> collector;

	public IsolatingCollector(Collector<T> collector) {
		this.collector = collector;
	}
	
	@Override
	public void startTransaction(T reference, Class<? extends TransactionHandler<T>> transactionHandlerClass) {
		collector.startTransaction(reference, transactionHandlerClass);
	}

	@Override
	public void execute(Object command) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void commitTransaction() {
		collector.commitTransaction();
	}

	@Override
	public void rejectTransaction() {
		collector.rejectTransaction();
	}

	@Override
	public void afterNextTrigger(Runnable runnable) {
		collector.afterNextTrigger(runnable);
	}
	
	@Override
	public void flushNextTrigger() {
		collector.flushNextTrigger();
	}
}
