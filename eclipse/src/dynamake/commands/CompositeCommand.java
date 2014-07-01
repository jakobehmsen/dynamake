package dynamake.commands;

import java.util.Date;

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
}
