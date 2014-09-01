package dynamake.commands;

import java.io.Serializable;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RelativeCommand<T> implements MappableCommand<T>, ForwardableCommand<T> {
	public static class Factory<T> implements ForwardableCommandFactory<T> {
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
			@SuppressWarnings("unchecked")
			RelativeCommand.Output<T> relCmdOutput = (RelativeCommand.Output<T>)output;
			Command<T> command = commandFactory.createCommand(relCmdOutput.commandOutput);
			return new RelativeCommand<T>(relCmdOutput.tail, command);
		}
		
		@Override
		public CommandFactory<T> forForwarding(Object output) {
			if(commandFactory instanceof ForwardableCommandFactory)
				return new RelativeCommand.Factory<T>(((ForwardableCommandFactory<T>)commandFactory).forForwarding(output));
			return this;
		}
	}
	
	public static class Output<T> implements Serializable, ForwardableOutput {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location<T> tail;
		public final Object commandOutput;
		
		public Output(Location<T> tail, Object commandOutput) {
			this.tail = tail;
			this.commandOutput = commandOutput;
		}

		@Override
		public Object forForwarding() {
			Location<T> newTail = tail.forForwarding();
			Object newCommandOutput;
			if(commandOutput instanceof ForwardableOutput)
				newCommandOutput = ((ForwardableOutput)commandOutput).forForwarding();
			else
				newCommandOutput = commandOutput;
			return new RelativeCommand.Output<T>(newTail, newCommandOutput);
		}
		
		@Override
		public String toString() {
			return "tail / " + commandOutput;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location<T> tail;
	private Command<T> command;

	public RelativeCommand(Location<T> tail, Command<T> command) {
		// TODO: Consider: How can mappable commands and mappable factories be supported?
		this.tail = tail;
		this.command = command;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		Location<T> commandLocation = new CompositeLocation<T>(location, tail);
		
		Object commandOutput = command.executeOn(propCtx, prevalentSystem, collector, commandLocation, scope);
		
		return new Output<T>(tail, commandOutput);
	}
	
	@Override
	public Command<T> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		if(command instanceof MappableCommand)
			return new RelativeCommand<T>(tail, ((MappableCommand<T>)command).mapToReferenceLocation(sourceReference, targetReference));

		return this;
	}
	
	@Override
	public Command<T> forForwarding(Object output) {
		Location<T> newTail = tail.forForwarding();
		Command<T> newCommand;
		
		if(command instanceof ForwardableCommand) {
			@SuppressWarnings("unchecked")
			RelativeCommand.Output<T> relativeCommandOutput = (RelativeCommand.Output<T>)output;
			newCommand = ((ForwardableCommand<T>)command).forForwarding(relativeCommandOutput.commandOutput);
		} else
			newCommand = command;

		return new RelativeCommand<T>(newTail, newCommand);
	}
	
	@Override
	public String toString() {
		return "tail / " + command;
	}
}
