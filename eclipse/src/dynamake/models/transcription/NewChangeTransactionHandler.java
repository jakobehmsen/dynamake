package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.commands.ForthPURCommand;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;
import dynamake.tuples.Tuple2;

public class NewChangeTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope<Model> scope;
	private ArrayList<Tuple2<ReversibleCommand<Model>, ExecutionScope<Model>>> newLog;

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
		newLog = new ArrayList<Tuple2<ReversibleCommand<Model>, ExecutionScope<Model>>>();
	}
	
	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		scope = new ExecutionScope<Model>();
	}

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		newLog.add(new Tuple2<ReversibleCommand<Model>, ExecutionScope<Model>>(command, scope));
		
		ArrayList<ReversibleCommand<Model>> pendingUndoablePairs = new ArrayList<ReversibleCommand<Model>>();
		pendingUndoablePairs.add(command);
		reference.appendLog(pendingUndoablePairs, propCtx, propDistance, collector);
	}

	@Override
	public void commitLogFor(Model reference) {
		if(newLog.size() > 0) {
			ArrayList<Tuple2<PURCommand<Model>, ExecutionScope<Model>>> newPurs = new ArrayList<Tuple2<PURCommand<Model>, ExecutionScope<Model>>>();
			for(Tuple2<ReversibleCommand<Model>, ExecutionScope<Model>> rcAndScope: newLog) {
				ReversibleCommand<Model> rc = rcAndScope.value1;
				PURCommand<Model> pur;
				if(rc instanceof PURCommand)
					pur = (PURCommand<Model>)rc;
				else
					pur = new ForthPURCommand<Model>(rc);
				newPurs.add(new Tuple2<PURCommand<Model>, ExecutionScope<Model>>(pur, rcAndScope.value2));
			}
			reference.commitLog(newPurs);
		}
	}

	@Override
	public void rejectLogFor(Model reference) {

	}
	
	@Override
	public ExecutionScope<Model> getScope() {
		return scope;
	}
}
