package dynamake.transcription;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public class SimplePendingCommandFactory<T> implements PendingCommandFactory<T> {
	private CommandState<T> pendingCommand;
	
	public SimplePendingCommandFactory(CommandState<T> pendingCommand) {
		this.pendingCommand = pendingCommand;
	}

	@Override
	public CommandState<T> createPendingCommand() {
		return pendingCommand;
	}
	
	@Override
	public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
}
