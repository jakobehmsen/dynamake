package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PendingCommandState<T> implements CommandState<T>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Command<T> command;
	private CommandFactory<T> forthFactory;
	private CommandFactory<T> backFactory;

	public PendingCommandState(final Command<T> forthCommand, final Command<T> backCommand) {
		this(forthCommand, 
			new ConstCommandFactory<T>(backCommand),
			new ConstCommandFactory<T>(forthCommand)
		);
	}

	public PendingCommandState(Command<T> command, CommandFactory<T> backFactory, CommandFactory<T> forthFactory) {
		this.command = command;
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	public PendingCommandState(CommandFactory<T> forthFactory, CommandFactory<T> backFactory) {
		this.command = forthFactory.createCommand(null);
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	public PendingCommandState(final Command<T> forthCommand, CommandFactory<T> backFactory) {
		this(forthCommand, 
			backFactory,
			new ConstCommandFactory<T>(forthCommand)
		);
	}

	public Command<T> getCommand() {
		return command;
	}

	public CommandFactory<T> getForthFactory() {
		return forthFactory;
	}

	public CommandFactory<T> getBackFactory() {
		// TODO Auto-generated method stub
		return backFactory;
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		Object output = command.executeOn(propCtx, prevalentSystem, collector, location);
//		System.out.println("Executed " + command);
		
		return new ReversibleCommand<T>(output, backFactory, forthFactory);
	}
	
	@Override
	public CommandState<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Command<T> newCommand;
		if(command instanceof MappableCommand)
			newCommand = ((MappableCommand<T>)command).mapToReferenceLocation(sourceReference, targetReference);
		else
			newCommand = command;
		
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
		
		return new PendingCommandState<>(newCommand, newBackFactory, newForthFactory);
	}
	
	@Override
	public CommandState<T> offset(Location offset) {
		Command<T> newCommand = new RelativeCommand<T>(offset, command);
		
		CommandFactory<T> newForthFactory = new RelativeCommand.Factory<T>(forthFactory);
		CommandFactory<T> newBackFactory = new RelativeCommand.Factory<T>(backFactory);
		
		return new PendingCommandState<>(newCommand, newBackFactory, newForthFactory);
	}
	
	@Override
	public CommandState<T> forForwarding() {
		// TODO Auto-generated method stub
		return null;
	}
}
