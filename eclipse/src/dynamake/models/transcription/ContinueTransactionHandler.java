package dynamake.models.transcription;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TransactionHandler;

/**
 * Each instance collects all executed commands and then outputs these commands into the parent scope.
 */
public class ContinueTransactionHandler<T> implements TransactionHandler<T> {
	private ExecutionScope<T> parentScope;
	private ExecutionScope<T> scope;
	private PURCommand<T> command;

	public ContinueTransactionHandler(ExecutionScope<T> scope, PURCommand<T> command) {
		this.scope = scope;
		this.command = command;
	}

	@Override
	public void startLogFor(TransactionHandler<T> parentHandler, T reference) {
//		// Assumes parent handler is available
		parentScope = parentHandler.getScope();
//		scope = new ExecutionScope<Model>();
//		newLog = new ArrayList<PURCommand<Model>>();
	}

	@Override
	public void logBeforeFor(T reference, Object command, PropogationContext propCtx, int propDistance, Collector<T> collector) { }

	@Override
	public void logFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector) {
//		newLog.add((PURCommand<Model>)command);
		
		// Assumes only command is executed
		
		// What should be the next of command?
		// TODO: Derive the next state of command
	}

	@Override
	public void commitLogFor(T reference) {
//		parentScope.produce(newLog);
//		parentScope.produce(scope);
		
		// TODO:
		// produce the next state of command
		// produce the new scope
		
		PURCommand<T> commandInNextState = command.inNextState();
		parentScope.produce(commandInNextState);
		parentScope.produce(scope);
	}

	@Override
	public void rejectLogFor(T reference) { }

	@Override
	public ExecutionScope<T> getScope() {
		return scope;
	}
}
