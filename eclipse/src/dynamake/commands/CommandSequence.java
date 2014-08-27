package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandSequence<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<ReversibleCommand<T>> commands;

	public CommandSequence(List<ReversibleCommand<T>> commands) {
		this.commands = commands;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
		collector.execute(commands);
		
		return null;
	}
}
