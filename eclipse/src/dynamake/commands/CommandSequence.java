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
	private List<ReversibleCommand<T>> commands;

	public CommandSequence(List<ReversibleCommand<T>> commands) {
		this.commands = commands;
	}

	@SafeVarargs
	public CommandSequence(ReversibleCommand<T>... commands) {
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
		ArrayList<ReversibleCommand<T>> commandsForForwarding = new ArrayList<ReversibleCommand<T>>();

		for(ReversibleCommand<T> command: commands) {
			commandsForForwarding.add(BaseValue.Util.forForwarding(command));
		}
		
		return new CommandSequence<T>(commandsForForwarding);
	}

	@Override
	public ReversibleCommand<T> forUpwarding() {
		ArrayList<ReversibleCommand<T>> commandsForForwarding = new ArrayList<ReversibleCommand<T>>();

		for(ReversibleCommand<T> command: commands)
			commandsForForwarding.add(BaseValue.Util.forUpwarding(command));
		
		return new CommandSequence<T>(commandsForForwarding);
	}

	@Override
	public ReversibleCommand<T> mapToReferenceLocation(T source, T target) {
		ArrayList<ReversibleCommand<T>> mappedCommands = new ArrayList<ReversibleCommand<T>>();
		
		for(ReversibleCommand<T> command: commands)
			mappedCommands.add(BaseValue.Util.mapToReferenceLocation(command, source, target));
		
		return new CommandSequence<T>(mappedCommands);
	}
}
