package dynamake.transcription;

public class IsolatingCollector<T> implements Collector<T> {
	private Collector<T> collector;

	public IsolatingCollector(Collector<T> collector) {
		this.collector = collector;
	}

	@Override
	public void execute(DualCommandFactory<T> transactionFactory) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void afterNextTrigger(Runnable runnable) {
		collector.afterNextTrigger(runnable);
	}

	@Override
	public void registerAffectedModel(T model) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void reject() {
		collector.reject();
	}

	@Override
	public void commit() {
		collector.commit();
	}

	@Override
	public void pushReference(T model) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void popReference() {
		// Do nothing which means side effects aren't collected
	}
}
