package dynamake.transcription;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.PendingUndoablePair;

public interface ExPendingCommandFactory2<T> {
	T getReference();
	PendingCommandState<T> createPendingCommand();
	void afterPropogationFinished(Model.PendingUndoablePair pendingUndoablePair, PropogationContext propCtx, int propDistance, Collector<T> collector);
	HistoryHandler<T> getHistoryHandler();
	
	public static class Util {
		public static <T> ExPendingCommandFactory2<T> sequence(final PendingCommandFactory<T> f) {
			final ArrayList<CommandState<T>> pendingCommands = new ArrayList<CommandState<T>>();
			f.createPendingCommands(pendingCommands);
			
			return new ExPendingCommandFactory2<T>() {
				int i;
				
				@Override
				public T getReference() {
					return f.getReference();
				}

				@Override
				public PendingCommandState<T> createPendingCommand() {
					return (PendingCommandState<T>)pendingCommands.get(i++);
				}

				@Override
				public void afterPropogationFinished(PendingUndoablePair pendingUndoablePair, PropogationContext propCtx, int propDistance, Collector<T> collector) {
					collector.execute(this);
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
					
					if(f instanceof ExPendingCommandFactory)
						historyHandler = ((ExPendingCommandFactory<T>)f).getHistoryHandler();

					return historyHandler;
				}
			};
		}
	}
}
