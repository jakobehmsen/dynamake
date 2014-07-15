package dynamake.commands;

import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PendingCommandState<T> implements CommandState<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Command2<T> command;
	private Command2Factory<T> forthFactory;
	private Command2Factory<T> backFactory;

	public PendingCommandState(final Command2<T> forthCommand, final Command2<T> backCommand) {
		this(forthCommand, 
			new Command2Factory<T>() {
				@Override
				public Command2<T> createCommand(Object outputs) {
					return backCommand;
				}
			},
			new Command2Factory<T>() {
				@Override
				public Command2<T> createCommand(Object outputs) {
					return forthCommand;
				}
			}
		);
	}

	public PendingCommandState(Command2<T> command, Command2Factory<T> backFactory, Command2Factory<T> forthFactory) {
		this.command = command;
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	public PendingCommandState(Command2Factory<T> forthFactory, Command2Factory<T> backFactory) {
		this.command = forthFactory.createCommand(null);
		this.forthFactory = forthFactory;
		this.backFactory = backFactory;
	}

	public PendingCommandState(final Command2<T> forthCommand, Command2Factory<T> backFactory) {
		this(forthCommand, 
			backFactory,
			new Command2Factory<T>() {
				@Override
				public Command2<T> createCommand(Object outputs) {
					return forthCommand;
				}
			}
		);
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector) {
		Object output = command.executeOn(propCtx, prevalentSystem, executionTime, collector);
		
		return new ReversibleCommand<T>(output, backFactory, forthFactory);
	}
}
