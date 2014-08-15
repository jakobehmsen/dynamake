package dynamake.transcription;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public interface ExPendingCommandFactory<T> {
	T getReference();
	void createPendingCommands(List<CommandState<T>> pendingCommands);
	void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	Class<? extends HistoryHandler<T>> getHistoryHandlerClass();
}
