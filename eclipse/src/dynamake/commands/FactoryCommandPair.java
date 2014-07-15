package dynamake.commands;

import java.util.ArrayList;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class FactoryCommandPair<T> implements DualCommand2<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CommandFactory<T> forthFactory;
	private CommandFactory<T> backFactory;

	public FactoryCommandPair(CommandFactory<T> forthFactory, CommandFactory<T> backFactory) {
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	@Override
	public DualCommand2<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector) {
		ArrayList<Command<T>> commands = new ArrayList<Command<T>>();
		forthFactory.createCommands(commands);
		
		// Reverse factories to create antagonistic command
		return new FactoryCommandPair<T>(backFactory, forthFactory);
	}
}
