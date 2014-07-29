package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RevertingCommandStateSequence<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private CommandState<T>[] commandStates;
	
	public RevertingCommandStateSequence(CommandState<T>[] commandStates) {
		this.commandStates = commandStates;
	}
	
	public static <T> RevertingCommandStateSequence<T> reverse(CommandState<T>[] commandStates) {
		@SuppressWarnings("unchecked")
		CommandState<T>[] newCommandStates = (CommandState<T>[])new CommandState[commandStates.length];
		
		for(int i = 0; i < commandStates.length; i++)
			newCommandStates[commandStates.length - 1 - i] = commandStates[i];
		
		return new RevertingCommandStateSequence<T>(newCommandStates);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> RevertingCommandStateSequence<T> reverse(List<CommandState<T>> commandStateList) {
		return reverse((CommandState<T>[])commandStateList.toArray(new CommandState[commandStateList.size()]));
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		// The new command states are reverted for undo/redo support
		@SuppressWarnings("unchecked")
		CommandState<T>[] newCommandStates = (CommandState<T>[])new CommandState[commandStates.length];
		
		for(int i = 0; i < commandStates.length; i++)
			newCommandStates[i] = commandStates[i].executeOn(propCtx, prevalentSystem, collector, location);

		return reverse(newCommandStates);
	}

	public int getCommandStateCount() {
		return commandStates.length;
	}

	public CommandState<T> getCommandState(int index) {
		return commandStates[index];
	}
	
	@Override
	public CommandState<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		@SuppressWarnings("unchecked")
		CommandState<T>[] newCommandStates = (CommandState<T>[])new CommandState[commandStates.length];
		for(int i = 0; i < commandStates.length; i++)
			newCommandStates[i] = commandStates[i].mapToReferenceLocation(sourceReference, targetReference);
		return new RevertingCommandStateSequence<T>(newCommandStates);
	}
}
