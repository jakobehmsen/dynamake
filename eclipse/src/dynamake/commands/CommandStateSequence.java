package dynamake.commands;

import java.util.ArrayList;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandStateSequence<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private CommandState<T>[] commandStates;
	
	public CommandStateSequence(CommandState<T>[] commandStates) {
		this.commandStates = commandStates;
	}
	
	@SuppressWarnings("unchecked")
	public CommandStateSequence(ArrayList<CommandState<T>> commandStateList) {
		commandStates = (CommandState<T>[])commandStateList.toArray(new CommandState[commandStateList.size()]);
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location) {
		@SuppressWarnings("unchecked")
		CommandState<T>[] newCommandStates = (CommandState<T>[])new CommandState[commandStates.length];
		
		for(int i = 0; i < commandStates.length; i++)
			newCommandStates[i] = commandStates[i].executeOn(propCtx, prevalentSystem, executionTime, collector, location);

		return new CommandStateSequence<T>(newCommandStates);
	}
}
