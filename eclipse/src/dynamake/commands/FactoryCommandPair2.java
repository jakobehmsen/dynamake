package dynamake.commands;

import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class FactoryCommandPair2<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//	Command to execute;
	//	Next state factory;

	public FactoryCommandPair2(CommandFactory<T> forthFactory, CommandFactory<T> backFactory) {
		// Set commands
		// Set factory to create next command state
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector) {
		// Execute command
		// Call and return next state command state from factory set in constructor
		
		return null;
	}
}
