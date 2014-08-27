package dynamake.transcription;


import dynamake.commands.ExecutionScope;
import dynamake.commands.ReversibleCommand;
import dynamake.models.PropogationContext;

public interface TransactionHandler<T> {
	void startLogFor(TransactionHandler<T> parentHandler, T reference);
	// Side-effects are valid here. Thus, collector parameter is needed to support this.
	void logFor(T reference, ReversibleCommand<T> command, PropogationContext propCtx, int propDistance, Collector<T> collector);
	void commitLogFor(T reference);
	void rejectLogFor(T reference);
	ExecutionScope getScope();
}
