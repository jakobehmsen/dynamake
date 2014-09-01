package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

public class PostOnlyTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope<Model> scope;
	
	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
		if(parentHandler != null)
			scope = parentHandler.getScope();
		else // In case, this transaction handler is the top handler
			scope = new ExecutionScope<Model>();
	}
	
	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {

	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// TODO: How to post a command?
//		reference.postLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference) {

	}
	
	@Override
	public void rejectLogFor(Model reference) {

	}
	
	@Override
	public ExecutionScope<Model> getScope() {
		return scope;
	}
}
