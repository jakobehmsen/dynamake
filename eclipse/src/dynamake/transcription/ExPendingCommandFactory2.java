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
			return new ExPendingCommandFactory2<T>() {
				ArrayList<CommandState<T>> pendingCommands;
				int i;
				
				@Override
				public T getReference() {
					return f.getReference();
				}

				@Override
				public PendingCommandState<T> createPendingCommand() {
					if(pendingCommands == null) {
						pendingCommands = new ArrayList<CommandState<T>>();
						f.createPendingCommands(pendingCommands);
					}
					
					if(i < pendingCommands.size())
						return (PendingCommandState<T>)pendingCommands.get(i++);
					return null;
				}

				@Override
				public void afterPropogationFinished(PendingUndoablePair pendingUndoablePair, PropogationContext propCtx, int propDistance, Collector<T> collector) {
					if(i < pendingCommands.size())
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
