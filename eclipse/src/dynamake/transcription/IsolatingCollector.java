package dynamake.transcription;

import dynamake.commands.Command;
import dynamake.commands.ReversibleCommandPair;

public class IsolatingCollector<T> implements Collector<T> {
	private Collector<T> collector;

	public IsolatingCollector(Collector<T> collector) {
		this.collector = collector;
	}
	
	@Override
	public void startTransaction(T reference, Object transactionHandlerClass) {
		collector.startTransaction(reference, transactionHandlerClass);
	}
	
	@Override
	public Object createProduceCommand(Object value) {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createConsumeCommand() {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createStoreCommand(String name) {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createLoadCommand(String name) {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createPushOffset() {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createPopOffset() {
		// Do nothing which means side effects aren't collected
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
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
