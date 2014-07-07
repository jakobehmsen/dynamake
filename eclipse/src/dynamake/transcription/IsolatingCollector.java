package dynamake.transcription;

public class IsolatingCollector<T> implements TranscriberCollector<T> {
	private TranscriberCollector<T> collector;

	public IsolatingCollector(TranscriberCollector<T> collector) {
		this.collector = collector;
	}
	
	@Override
	public void execute(DualCommandFactory<T> transactionFactory) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void enlist(DualCommandFactory<T> transactionFactory) {
		// Do nothing which means side effects aren't collected
	}

	@Override
	public void afterNextFlush(TranscriberOnFlush<T> runnable) {
		collector.afterNextFlush(runnable);
	}

	@Override
	public void registerAffectedModel(T model) {
//		collector.registerAffectedModel(model);
	}

	@Override
	public void enlistReject() {
		collector.enlistReject();
	}

	@Override
	public void enlistCommit() {
		collector.enlistCommit();
	}
	
	@Override
	public void flush() {
		collector.flush();
	}
}
