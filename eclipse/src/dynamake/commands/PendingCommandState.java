package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
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
			new CommandFactory<T>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Command<T> createCommand(Object outputs) {
					return backCommand;
				}
			},
			new CommandFactory<T>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Command<T> createCommand(Object outputs) {
					return forthCommand;
				}
			}
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
			new CommandFactory<T>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public Command<T> createCommand(Object outputs) {
					return forthCommand;
				}
			}
		);
	}

	@Override
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location) {
		Object output = command.executeOn(propCtx, prevalentSystem, collector, location);
		
		return new ReversibleCommand<T>(output, backFactory, forthFactory);
	}
}
