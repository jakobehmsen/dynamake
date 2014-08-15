package dynamake.transcription;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public class SimpleExPendingCommandFactory2<T> implements ExPendingCommandFactory2<T> {
	private T reference;
	private CommandState<T> pendingCommand;
	
	public SimpleExPendingCommandFactory2(T reference, CommandState<T> pendingCommand) {
		if(reference == null)
			new String();
		this.reference = reference;
		this.pendingCommand = pendingCommand;
	}

	@Override
	public T getReference() {
		return reference;
	}

	@Override
	public CommandState<T> createPendingCommand() {
		return pendingCommand;
	}
	
	@Override
	public void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
		return (Class<? extends HistoryHandler<T>>) NullHistoryHandler.class;
	}
}
