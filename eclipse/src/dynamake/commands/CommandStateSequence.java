package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandStateSequence<Model> implements CommandState<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<CommandState<Model>> commandStates;

	public CommandStateSequence(List<CommandState<Model>> commandStates) {
		this.commandStates = commandStates;
	}

	@Override
	public CommandState<Model> executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		for(CommandState<Model> commandState: commandStates)
			commandState.executeOn(propCtx, prevalentSystem, collector, location);
		
		return this;
	}
}
