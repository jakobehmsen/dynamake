package dynamake.transcription;

import dynamake.commands.Command;
import dynamake.commands.ReversibleCommandPair;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void startTransaction(T reference, Object transactionHandlerClass) { }
	
	@Override
	public Object createProduceCommand(Object value) {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createConsumeCommand() {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createPushOffset() {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public Object createPopOffset() {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
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
