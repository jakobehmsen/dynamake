package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ReversibleCommand<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object output;
	private Command2Factory<T> forthFactory;
	private Command2Factory<T> backFactory;

	public ReversibleCommand(Object output, Command2Factory<T> forthFactory, Command2Factory<T> backFactory) {
		this.output = output;
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location) {
		Command2<T> newCommand = forthFactory.createCommand(output);
		
		Object newOutput = newCommand.executeOn(propCtx, prevalentSystem, executionTime, collector, location);
		
		// Reverse factories to create antagonistic command
		return new ReversibleCommand<T>(newOutput, backFactory, forthFactory);
	}
}
