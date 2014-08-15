package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.models.PropogationContext;

public interface ExPendingCommandFactory2<T> {
	T getReference();
	CommandState<T> createPendingCommand();
	void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector);
	Class<? extends HistoryHandler<T>> getHistoryHandlerClass();
	
	public static class Util {
		public static <T> ExPendingCommandFactory2<T> sequence(final PendingCommandFactory<T> f) {
			return new ExPendingCommandFactory2<T>() {
				int i = 0;
				ArrayList<CommandState<T>> createdPendingCommands;
				
				@Override
				public T getReference() {
					return f.getReference();
				}
				
				@Override
				public CommandState<T> createPendingCommand() {
					if(createdPendingCommands == null) {
						createdPendingCommands = new ArrayList<CommandState<T>>();
						// Assumed that at least one pending command is created
						f.createPendingCommands(createdPendingCommands);
					}

					return createdPendingCommands.get(i);
				}

				@Override
				public void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {
					i++;
					if(i < createdPendingCommands.size())
						collector.execute(this);
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
		
		public static <T> void sequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands) {
			sequence(collector, reference, pendingCommands, 0);
		}
		
		private static <T> void sequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, final int i) {
			if(i < pendingCommands.size()) {
				collector.execute(new SimpleExPendingCommandFactory2<T>(reference, pendingCommands.get(i)) {
					@Override
					public void afterPropogationFinished(List<Execution<T>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<T> collector) {
						sequence(collector, reference, pendingCommands, i + 1);
					}
				});
			}
		}
	}
}
