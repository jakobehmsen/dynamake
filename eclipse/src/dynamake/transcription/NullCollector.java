package dynamake.transcription;

public class NullCollector<T> implements Collector<T> {
	@Override
	public void execute(Object command) { }

	@Override
	public void afterNextTrigger(Runnable runnable) { }

	@Override
	public void reject() { }

	@Override
	public void commit() { }
	
	@Override
	public void flushNextTrigger() { }
}
