package dynamake.transcription;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public class SimplePendingCommandFactory<T> implements PendingCommandFactory<T> {
	private T reference;
	private CommandState<T> pendingCommand;
	
	public SimplePendingCommandFactory(T reference, CommandState<T> pendingCommand) {
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
	public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends TransactionHandler<T>> getTransactionHandlerClass() {
		return (Class<? extends TransactionHandler<T>>) NullTransactionHandler.class;
	}
}
