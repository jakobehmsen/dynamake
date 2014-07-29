package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ReversibleCommand<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object output;
	private CommandFactory<T> forthFactory;
	private CommandFactory<T> backFactory;

	public ReversibleCommand(Object output, CommandFactory<T> forthFactory, CommandFactory<T> backFactory) {
		this.output = output;
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}
	
	public Object getOutput() {
		return output;
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		Command<T> newCommand = forthFactory.createCommand(output);
		
		Object newOutput = newCommand.executeOn(propCtx, prevalentSystem, collector, location);
		
		// Reverse factories to create antagonistic command
		return new ReversibleCommand<T>(newOutput, backFactory, forthFactory);
	}
	
	@Override
	public CommandState<T> mapToReferenceLocation(Location referenceLocation) {
		Object newOutput;
		if(output instanceof MappableOutput)
			newOutput = ((MappableOutput)output).mapToReferenceLocation(referenceLocation);
		else
			newOutput = output;
		
		CommandFactory<T> newForthFactory;
		if(forthFactory instanceof MappableCommandFactory)
			newForthFactory = ((MappableCommandFactory<T>)forthFactory).mapToReferenceLocation(referenceLocation);
		else
			newForthFactory = forthFactory;
		
		CommandFactory<T> newBackFactory;
		if(backFactory instanceof MappableCommandFactory)
			newBackFactory = ((MappableCommandFactory<T>)backFactory).mapToReferenceLocation(referenceLocation);
		else
			newBackFactory = backFactory;
		
		return new ReversibleCommand<T>(newOutput, newForthFactory, newBackFactory);
	}
}
