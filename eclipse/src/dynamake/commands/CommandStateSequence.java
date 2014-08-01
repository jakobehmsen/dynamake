package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CommandStateSequence<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<CommandState<T>> commandStates;

	public CommandStateSequence(List<CommandState<T>> commandStates) {
		this.commandStates = commandStates;
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		for(CommandState<T> commandState: commandStates)
			commandState.executeOn(propCtx, prevalentSystem, collector, location);
		
		return this;
	}
	
	@Override
	public CommandState<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		ArrayList<CommandState<T>> newCommandStates = new ArrayList<CommandState<T>>();
		
		for(CommandState<T> commandState: commandStates) {
			CommandState<T> newCommandState = commandState.mapToReferenceLocation(sourceReference, targetReference);
			newCommandStates.add(newCommandState);
		}
		
		return new CommandStateSequence<T>(newCommandStates);
	}
	
	@Override
	public CommandState<T> offset(Location offset) {
		ArrayList<CommandState<T>> newCommandStates = new ArrayList<CommandState<T>>();
		
		for(CommandState<T> commandState: commandStates) {
			CommandState<T> newCommandState = commandState.offset(offset);
			newCommandStates.add(newCommandState);
		}
		
		return new CommandStateSequence<T>(newCommandStates);
	}
}
