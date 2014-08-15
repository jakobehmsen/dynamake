package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.models.PropogationContext;

public interface ExPendingCommandFactory2<T> {
	T getReference();
	CommandState<T> createPendingCommand();
	void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector);
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
				public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
					i++;
					if(i < createdPendingCommands.size())
						collector.execute(this);
				}

				@SuppressWarnings("unchecked")
				@Override
				public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
					if(f instanceof TranscribeOnlyPendingCommandFactory)
						return (Class<? extends HistoryHandler<T>>)PostOnlyHistoryHandler.class;
					else
						return (Class<? extends HistoryHandler<T>>)LocalHistoryHandler.class;
				}
			};
		}
		
		public static <T> ExPendingCommandFactory2<T> sequence(final ExPendingCommandFactory<T> f) {
			return new ExPendingCommandFactory2<T>() {
				int i = 0;
				ArrayList<CommandState<T>> createdPendingCommands;
				ArrayList<Execution<T>> pendingUndoablePairs;
				
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
						pendingUndoablePairs = new ArrayList<Execution<T>>();
					}

					return createdPendingCommands.get(i);
				}

				@Override
				public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
					i++;
					if(i < createdPendingCommands.size()) {
						pendingUndoablePairs.add(execution);
						collector.execute(this);
					} else
						f.afterPropogationFinished(pendingUndoablePairs, propCtx, propDistance, collector);
				}

				@Override
				public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
					return f.getHistoryHandlerClass();
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
					public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
						sequence(collector, reference, pendingCommands, i + 1);
					}
				});
			}
		}
	}
}
