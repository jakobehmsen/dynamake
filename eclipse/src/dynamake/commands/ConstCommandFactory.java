package dynamake.commands;

import dynamake.models.Model;

public class ConstCommandFactory<T> implements MappableCommandFactory<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Command<T> command;

	public ConstCommandFactory(Command<T> command) {
		this.command = command;
	}

	@Override
	public Command<T> createCommand(Object output) {
		return command;
	}
	
	@Override
	public CommandFactory<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		if(command instanceof MappableCommand)
			return new ConstCommandFactory<T>(((MappableCommand<T>)command).mapToReferenceLocation(sourceReference, targetReference));
		return this;
	}
	
	public static <T> ConstCommandFactory<T> forNull() {
		return new ConstCommandFactory<T>(new Command.Null<T>());
	}
}
