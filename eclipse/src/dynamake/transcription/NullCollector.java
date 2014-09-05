package dynamake.transcription;

import dynamake.commands.Command;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.ReversibleCommandPair;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void startTransaction(T reference, Object transactionHandlerClass) { }
	
	@Override
	public ReversibleCommand<T> createProduceCommand(Object value) {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public ReversibleCommand<T> createConsumeCommand() {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public ReversibleCommand<T> createStoreCommand(String name) {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public ReversibleCommand<T> createLoadCommand(String name) {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public ReversibleCommand<T> createPushOffset() {
		return new ReversibleCommandPair<T>(new Command.Null<T>(), new Command.Null<T>());
	}
	
	@Override
	public ReversibleCommand<T> createPopOffset() {
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
