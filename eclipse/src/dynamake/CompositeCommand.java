package dynamake;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;

public class CompositeCommand<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Command<T>[] commandSequence;

	public CompositeCommand(Command<T>[] commandSequence) {
		this.commandSequence = commandSequence;
	}

	@Override
	public void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberBranch<T> branch) {
		for(Command<T> command: commandSequence) {
			command.executeOn(propCtx, prevalentSystem, executionTime, branch);
		}
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		// Assumed that result is the same for all commands in commandSequence
		return commandSequence[0].occurredWithin(location);
	}
}
