package dynamake.transcription;

import java.util.Arrays;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public class SimpleExPendingCommandFactory<T> implements ExPendingCommandFactory<T> {
	private T reference;
	private List<CommandState<T>> pendingCommands;
	
	public SimpleExPendingCommandFactory(T reference, List<CommandState<T>> pendingCommands) {
		if(reference == null)
			new String();
		if(pendingCommands.size() == 0)
			new String();
		this.reference = reference;
		this.pendingCommands = pendingCommands;
	}
	
	@SafeVarargs
	public SimpleExPendingCommandFactory(T reference, CommandState<T>... pendingCommands) {
		if(reference == null)
			new String();
		if(pendingCommands.length == 0)
			new String();
		this.reference = reference;
		this.pendingCommands = Arrays.asList(pendingCommands);
	}
	
	public SimpleExPendingCommandFactory() {
		
	}

	@Override
	public T getReference() {
		return reference;
	}
	
	@Override
	public void createPendingCommands(List<CommandState<T>> pendingCommands) {
		pendingCommands.addAll(this.pendingCommands);
	}
	
	@Override
	public void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
		return (Class<? extends HistoryHandler<T>>) NullHistoryHandler.class;
	}
	
//	@Override
//	public HistoryHandler<T> getHistoryHandler() {
//		return new NullHistoryHandler<T>();
//	}
}
