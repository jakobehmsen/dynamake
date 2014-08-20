package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public interface PendingCommandFactory<T> {
	T getReference();
	CommandState<T> createPendingCommand();
	void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector);
	Class<? extends HistoryHandler<T>> getHistoryHandlerClass();
	
	public static class Util {
		public static <T> void executeSingle(Collector<T> collector, final T reference, final Class<? extends HistoryHandler<T>> historyHandlerClass, final CommandState<T> pendingCommand) {
			collector.execute(new SimplePendingCommandFactory<T>(reference, pendingCommand) {
				@Override
				public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
					return historyHandlerClass;
				}
			});
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands) {
			executeSequence(collector, reference, pendingCommands, new ExecutionsHandler<T>() {
				@Override
				public void handleExecutions(List<Execution<T>> executions, Collector<T> collector) { }
			});
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, Class<? extends HistoryHandler<T>> historyHandlerClass) {
			executeSequence(collector, reference, pendingCommands, historyHandlerClass, new ExecutionsHandler<T>() {
				@Override
				public void handleExecutions(List<Execution<T>> executions, Collector<T> collector) { }
			});
		}
		
		@SuppressWarnings("unchecked")
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, ExecutionsHandler<T> afterExecutions) {
			executeSequence(collector, reference, pendingCommands, (Class<? extends HistoryHandler<T>>)NullHistoryHandler.class, afterExecutions, new ArrayList<Execution<T>>(), 0);
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, Class<? extends HistoryHandler<T>> historyHandlerClass, ExecutionsHandler<T> afterExecutions) {
			executeSequence(collector, reference, pendingCommands, historyHandlerClass, afterExecutions, new ArrayList<Execution<T>>(), 0);
		}
		
		private static <T> void executeSequence(
				Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, final Class<? extends HistoryHandler<T>> historyHandlerClass, 
				final ExecutionsHandler<T> afterExecutions, final List<Execution<T>> executions, final int i) {
			if(i < pendingCommands.size()) {
				collector.execute(new SimplePendingCommandFactory<T>(reference, pendingCommands.get(i)) {
					@Override
					public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
						executions.add(execution);
						executeSequence(collector, reference, pendingCommands, historyHandlerClass, afterExecutions, executions, i + 1);
					}
					
					@Override
					public Class<? extends HistoryHandler<T>> getHistoryHandlerClass() {
						return historyHandlerClass;
					}
				});
			} else {
				afterExecutions.handleExecutions(executions, collector);
			}
		}
	}
}
