package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

public class UndoTransactionHandler implements TransactionHandler<Model> {
	private Model.HistoryPart undoPart;
	private int partIndex;
	private ExecutionScope scope;
	
	public UndoTransactionHandler(Model.HistoryPart undoPart) {
		this.undoPart = undoPart;
	}
	
	public UndoTransactionHandler() { }
	
	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		scope = undoPart.getScope(partIndex);
	}

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
//		System.out.println(this +  ": startLogFor");
	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println(this +  ": logFor");
		partIndex++;
	}

	@Override
	public void commitLogFor(Model reference) {
//		System.out.println(this +  ": commitLogFor");
		
		reference.commitUndo();
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
