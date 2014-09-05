package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ForthCommand<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ReversibleCommand<T> command;

	public ForthCommand(ReversibleCommand<T> command) {
		this.command = command;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		command.executeForward(propCtx, prevalentSystem, collector, location, scope);
		
		return null;
	}
}
