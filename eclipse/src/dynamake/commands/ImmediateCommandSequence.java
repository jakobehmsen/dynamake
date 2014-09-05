package dynamake.commands;

import java.util.Arrays;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ImmediateCommandSequence<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<ReversibleCommand<T>> commands;

	public ImmediateCommandSequence(List<ReversibleCommand<T>> commands) {
		this.commands = commands;
	}

	@SafeVarargs
	public ImmediateCommandSequence(ReversibleCommand<T>... commands) {
		this.commands = Arrays.asList(commands);
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		for(ReversibleCommand<T> command: commands) {
			command.executeForward(propCtx, prevalentSystem, collector, location, scope);
		}
		
		return null;
	}
}
