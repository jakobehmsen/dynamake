package dynamake.transcription;

import dynamake.commands.PendingCommandFactory;

public interface ExPendingCommandFactory<T> extends PendingCommandFactory<T>{
	HistoryHandler<T> getHistoryHandler();
}
