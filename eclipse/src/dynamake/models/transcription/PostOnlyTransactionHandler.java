package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.Execution;
import dynamake.transcription.TransactionHandler;

public class PostOnlyTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope scope;
	
	@Override
	public void startLogFor(Model reference) {
		scope = new ExecutionScope();
	}

	@Override
	public void logFor(Model reference, ArrayList<Execution<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.postLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference) {

	}
	
	@Override
	public void rejectLogFor(Model reference) {

	}
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
