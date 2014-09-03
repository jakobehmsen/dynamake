package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.models.transcription.ContinueTransactionHandlerFactory;
import dynamake.transcription.Collector;

public class ExecuteContinuationsFromScopeCommand<T> implements Command<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int continuationCount;

	public ExecuteContinuationsFromScopeCommand(int continuationCount) {
		this.continuationCount = continuationCount;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location<T> location, ExecutionScope<T> scope) {
		T reference = location.getChild(prevalentSystem);
		
		for(int i = 0; i < continuationCount; i++) {
			@SuppressWarnings("unchecked")
			ExecutionScope<T> continuationScope = (ExecutionScope<T>)scope.consume();
			@SuppressWarnings("unchecked")
			PURCommand<T> continuationCommand = (PURCommand<T>)scope.consume();
			
			collector.startTransaction(reference, new ContinueTransactionHandlerFactory<T>(continuationScope, continuationCommand));
			// Just that one command is to be executed
			collector.execute(continuationCommand);
			collector.commitTransaction();
		}

		return null;
	}
}
