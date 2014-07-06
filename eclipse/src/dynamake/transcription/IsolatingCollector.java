package dynamake.transcription;

public class IsolatingCollector<T> implements TranscriberCollector<T> {
	private TranscriberCollector<T> collector;

	public IsolatingCollector(TranscriberCollector<T> collector) {
		this.collector = collector;
	}

	@Override
	public void enqueue(DualCommandFactory<T> transactionFactory) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void afterNextFlush(TranscriberRunnable<T> runnable) {
		collector.afterNextFlush(runnable);
	}

	@Override
	public void registerAffectedModel(T model) {
		collector.registerAffectedModel(model);
	}

	@Override
	public void reject() {
		collector.reject();
	}

	@Override
	public void commit() {
		collector.commit();
	}
}
