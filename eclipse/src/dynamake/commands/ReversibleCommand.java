package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ReversibleCommand<T> implements CommandStateWithOutput<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Command<T> cause;
	private Object output;
	private CommandFactory<T> forthFactory;
	private CommandFactory<T> backFactory;

	public ReversibleCommand(Command<T> cause, Object output, CommandFactory<T> forthFactory, CommandFactory<T> backFactory) {
		this.cause = cause;
		this.output = output;
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}
	
	public Command<T> getCause() {
		return cause;
	}
	
	public Object getOutput() {
		return output;
	}

	public CommandFactory<T> getForthFactory() {
		return forthFactory;
	}
	
	public CommandFactory<T> getBackFactory() {
		return backFactory;
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		Command<T> newCommand = forthFactory.createCommand(output);
		
		Object newOutput = newCommand.executeOn(propCtx, prevalentSystem, collector, location);
		
		// Reverse factories to create antagonistic command
		return new ReversibleCommand<T>(newCommand, newOutput, backFactory, forthFactory);
	}
	
	@Override
	public CommandState<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Command<T> newCause;
		if(cause instanceof MappableCommand)
			newCause = ((MappableCommand<T>)cause).mapToReferenceLocation(sourceReference, targetReference);
		else
			newCause = cause;
		
		Object newOutput;
		if(output instanceof MappableOutput)
			newOutput = ((MappableOutput)output).mapToReferenceLocation(sourceReference, targetReference);
		else
			newOutput = output;
		
		CommandFactory<T> newForthFactory;
		if(forthFactory instanceof MappableCommandFactory)
			newForthFactory = ((MappableCommandFactory<T>)forthFactory).mapToReferenceLocation(sourceReference, targetReference);
		else
			newForthFactory = forthFactory;
		
		CommandFactory<T> newBackFactory;
		if(backFactory instanceof MappableCommandFactory)
			newBackFactory = ((MappableCommandFactory<T>)backFactory).mapToReferenceLocation(sourceReference, targetReference);
		else
			newBackFactory = backFactory;
		
		return new ReversibleCommand<T>(newCause, newOutput, newForthFactory, newBackFactory);
	}
	
	@Override
	public CommandState<T> offset(Location offset) {
		Command<T> newCause = new RelativeCommand<T>(offset, cause);
		Object newOutput = new RelativeCommand.Output(offset, output);
		
		CommandFactory<T> newForthFactory = new RelativeCommand.Factory<T>(forthFactory);
		CommandFactory<T> newBackFactory = new RelativeCommand.Factory<T>(backFactory);
		
		return new ReversibleCommand<T>(newCause, newOutput, newForthFactory, newBackFactory);
	}
	
	@Override
	public CommandState<T> forForwarding() {
		CommandFactory<T> newForthFactory = forthFactory;
		Object newOutput = output;
		
		if(newForthFactory instanceof ForwardableCommandFactory)
			newForthFactory = ((ForwardableCommandFactory<T>)newForthFactory).forForwarding(output);
		
		if(output instanceof ForwardableOutput)
			newOutput = ((ForwardableOutput)output).forForwarding();
		
		return new ReversibleCommand<T>(cause, newOutput, newForthFactory, backFactory);
	}
	
	@Override
	public void appendPendings(List<CommandState<T>> pendingCommands) {
		
	}

	@Override
	public CommandState<T> forForwarding(Object output) {
		return null;
	}
}
