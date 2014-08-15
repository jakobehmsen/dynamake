package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.Execution;
import dynamake.transcription.SimpleExPendingCommandFactory;

public class ChainCommand<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PendingCommandState<T> firstPending;
	private List<CommandState<T>> secondPending;

	public ChainCommand(PendingCommandState<T> firstPending, List<CommandState<T>> secondPending) {
		this.firstPending = firstPending;
		this.secondPending = secondPending;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		@SuppressWarnings("unchecked")
		final T reference = (T)location.getChild(prevalentSystem);
		// Wait executing second pending till first pending, and all of its side effects, have finished
		collector.execute(new SimpleExPendingCommandFactory<T>(reference, firstPending) {
			@Override
			public void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {
				collector.execute(new SimpleExPendingCommandFactory<T>(reference, secondPending));
			}
		});
		return null;
	}
}
