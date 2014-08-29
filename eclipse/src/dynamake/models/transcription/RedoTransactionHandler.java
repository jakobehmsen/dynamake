package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

public class RedoTransactionHandler implements TransactionHandler<Model> {
	private Model.HistoryPart redoPart;
	private int partIndex;
	private ExecutionScope scope;
	
	public RedoTransactionHandler(Model.HistoryPart undoPart) {
		this.redoPart = undoPart;
	}
	
	public RedoTransactionHandler() { }
	
	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		scope = redoPart.getScope(partIndex);
	}

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {

	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
		partIndex++;
	}

	@Override
	public void commitLogFor(Model reference) {
//		System.out.println(this +  ": commitLogFor");
		
		reference.commitRedo();
	}

	@Override
	public void rejectLogFor(Model reference) {
//		System.out.println(this +  ": rejectLogFor");
	}
	
	@Override
	public ExecutionScope getScope() {
		return scope;
	}
}
