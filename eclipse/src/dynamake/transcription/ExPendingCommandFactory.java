package dynamake.transcription;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public interface ExPendingCommandFactory<T> {
	T getReference();
	void createPendingCommands(List<CommandState<T>> pendingCommands);
	void afterPropogationFinished(List<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	Class<? extends HistoryHandler<T>> getHistoryHandlerClass();
	
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
				public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
					if(f instanceof TranscribeOnlyAndPostNotPendingCommandFactory)
						return (Class<? extends HistoryHandler<T>>)NullHistoryHandler.class;
					else if(f instanceof TranscribeOnlyPendingCommandFactory)
						return (Class<? extends HistoryHandler<T>>)PostOnlyHistoryHandler.class;
					else
						return (Class<? extends HistoryHandler<T>>)LocalHistoryHandler.class;
				}
			};
		}
	}
}
