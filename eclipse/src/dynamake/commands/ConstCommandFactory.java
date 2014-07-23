package dynamake.commands;

public class ConstCommandFactory<T> implements CommandFactory<T> {
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
}
