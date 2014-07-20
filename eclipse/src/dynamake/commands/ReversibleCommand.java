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

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		Command<T> newCommand = forthFactory.createCommand(output);
		
		Object newOutput = newCommand.executeOn(propCtx, prevalentSystem, collector, location);
		
		// Reverse factories to create antagonistic command
		return new ReversibleCommand<T>(newOutput, backFactory, forthFactory);
	}
}
