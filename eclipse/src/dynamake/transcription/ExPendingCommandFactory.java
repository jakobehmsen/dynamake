package dynamake.transcription;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public interface ExPendingCommandFactory<T> {
	T getReference();
	void createPendingCommands(List<CommandState<T>> pendingCommands);
	void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	HistoryHandler<T> getHistoryHandler();
	
	public static class Util {
		public static <T> ExPendingCommandFactory<T> sequence(final PendingCommandFactory<T> f) {
			return new ExPendingCommandFactory<T>() {
				@Override
				public T getReference() {
					return f.getReference();
				}

				@Override
				public void createPendingCommands(List<CommandState<T>> pendingCommands) {
					f.createPendingCommands(pendingCommands);
				}

				@Override
				public void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {

				}

				@SuppressWarnings("unchecked")
				@Override
				public HistoryHandler<T> getHistoryHandler() {
					HistoryHandler<T> historyHandler;
					
					if(f instanceof TranscribeOnlyAndPostNotPendingCommandFactory)
						historyHandler = new NullHistoryHandler<T>();
					else if(f instanceof TranscribeOnlyPendingCommandFactory)
						historyHandler = (HistoryHandler<T>)new PostOnlyHistoryHandler();
					else
						historyHandler = (HistoryHandler<T>)new LocalHistoryHandler();

					return historyHandler;
				}
			};
		}
	}
}
