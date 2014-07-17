package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RelativeCommand<T> implements Command<T> {
	public static class Factory<T> implements CommandFactory<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private CommandFactory<T> commandFactory;

		public Factory(CommandFactory<T> commandFactory) {
			this.commandFactory = commandFactory;
		}

		@Override
		public Command<T> createCommand(Object output) {
			RelativeCommand.Output relCmdOutput = (RelativeCommand.Output)output;
			Command<T> command = commandFactory.createCommand(relCmdOutput.commandOutput);
			return new RelativeCommand<T>(relCmdOutput.tail, command);
		}
	}
	
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location tail;
		public final Object commandOutput;
		
		public Output(Location tail, Object commandOutput) {
			this.tail = tail;
			this.commandOutput = commandOutput;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location tail;
	private Command<T> command;

	public RelativeCommand(Location tail, Command<T> command) {
		this.tail = tail;
		this.command = command;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location) {
		Location commandLocation = new CompositeModelLocation(location, tail);
		
		Object commandOutput = command.executeOn(propCtx, prevalentSystem, executionTime, collector, commandLocation);
		
		return new Output(tail, commandOutput);
	}
}
