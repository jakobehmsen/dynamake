package dynamake.models.transcription;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

/**
 * Each instance collects all executed commands and then outputs these commands into the parent scope.
 */
public class ContinueTransactionHandler implements TransactionHandler<Model> {
	private ExecutionScope<Model> scope;
	private PURCommand<Model> command;

	public ContinueTransactionHandler(ExecutionScope<Model> scope, PURCommand<Model> command) {
		this.scope = scope;
		this.command = command;
	}

	@Override
	public void startLogFor(TransactionHandler<Model> parentHandler, Model reference) {
//		// Assumes parent handler is available
//		parentScope = parentHandler.getScope();
//		scope = new ExecutionScope<Model>();
//		newLog = new ArrayList<PURCommand<Model>>();
	}

	@Override
	public void logBeforeFor(Model reference, Object command, PropogationContext propCtx, int propDistance, Collector<Model> collector) { }

	@Override
	public void logFor(Model reference, ReversibleCommand<Model> command, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		newLog.add((PURCommand<Model>)command);
		
		// Assumes only command is executed
		
		// What should be the next of command?
		// TODO: Derive the next state of command
	}

	@Override
	public void commitLogFor(Model reference) {
//		parentScope.produce(newLog);
//		parentScope.produce(scope);
		
		// TODO:
		// produce the next state of command
		// produce the new scope
	}

	@Override
	public void rejectLogFor(Model reference) { }

	@Override
	public ExecutionScope<Model> getScope() {
		return scope;
	}
}
