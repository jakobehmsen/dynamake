package dynamake.transcription;

import dynamake.commands.PendingCommandState;
import dynamake.models.Model;

public interface ExPendingCommandFactory2<T> {
	T getReference();
	PendingCommandState<T> createPendingCommand();
	void afterPropogationFinished(Model.PendingUndoablePair pendingUndoablePair);
	HistoryHandler<T> getHistoryHandler();
}
