package dynamake.transcription;

import java.util.Arrays;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public class SimpleExPendingCommandFactory2<T> implements ExPendingCommandFactory<T> {
	private T reference;
	private List<CommandState<T>> pendingCommands;
	
	public SimpleExPendingCommandFactory2(T reference, List<CommandState<T>> pendingCommands) {
		if(reference == null)
			new String();
		this.reference = reference;
		this.pendingCommands = pendingCommands;
	}
	
	@SafeVarargs
	public SimpleExPendingCommandFactory2(T reference, CommandState<T>... pendingCommands) {
		if(reference == null)
			new String();
		this.reference = reference;
		this.pendingCommands = Arrays.asList(pendingCommands);
	}
	
	public SimpleExPendingCommandFactory2() {
		
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
	public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {

	}
	
	@Override
	public HistoryHandler<T> getHistoryHandler() {
		return new NullHistoryHandler<T>();
	}
}
