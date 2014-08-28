package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandSequence<T> implements ReversibleCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<? extends Object> commands;

	public CommandSequence(List<? extends Object> commands) {
		this.commands = commands;
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
		collector.execute(commands);
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
		// Each of the executed commands is assumed to know how reverse itself
	}
}
