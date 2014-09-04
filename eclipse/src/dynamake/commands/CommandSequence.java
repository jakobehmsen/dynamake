package dynamake.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandSequence<T> implements ReversibleCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Object> commands;

	public CommandSequence(List<Object> commands) {
		this.commands = commands;
	}

	@SafeVarargs
	public CommandSequence(Object... commands) {
		this.commands = Arrays.asList(commands);
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		collector.execute(commands);
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		// Each of the executed commands is assumed to know how reverse itself
	}

	@Override
	public ReversibleCommand<T> forForwarding() {
		ArrayList<Object> commandsForForwarding = new ArrayList<Object>();

		for(Object command: commands)
			commandsForForwarding.add(BaseValue.Util.forForwarding(command));
		
		return new CommandSequence<T>(commandsForForwarding);
	}

	@Override
	public ReversibleCommand<T> forUpwarding() {
		ArrayList<Object> commandsForForwarding = new ArrayList<Object>();

		for(Object command: commands)
			commandsForForwarding.add(BaseValue.Util.forUpwarding(command));
		
		return new CommandSequence<T>(commandsForForwarding);
	}

	@Override
	public ReversibleCommand<T> mapToReferenceLocation(T source, T target) {
		ArrayList<Object> mappedCommands = new ArrayList<Object>();
		
		for(Object command: commands)
			mappedCommands.add(BaseValue.Util.mapToReferenceLocation(command, source, target));
		
		return new CommandSequence<T>(mappedCommands);
	}
}
