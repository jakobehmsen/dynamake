package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.ModelLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RelativeCommand<T> implements Command2<T> {
	public static class Factory<T> implements Command2Factory<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Command2Factory<T> commandFactory;

		public Factory(Command2Factory<T> commandFactory) {
			this.commandFactory = commandFactory;
		}

		@Override
		public Command2<T> createCommand(Object output) {
			RelativeCommand.Output relCmdOutput = (RelativeCommand.Output)output;
			Command2<T> command = commandFactory.createCommand(relCmdOutput.commandOutput);
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
	private Command2<T> command;

	public RelativeCommand(Location tail, Command2<T> command) {
		this.tail = tail;
		this.command = command;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location) {
		Location commandLocation = new CompositeModelLocation((ModelLocation)location, (ModelLocation)tail);
		
		Object commandOutput = command.executeOn(propCtx, prevalentSystem, executionTime, collector, commandLocation);
		
		return new Output(tail, commandOutput);
	}
}
