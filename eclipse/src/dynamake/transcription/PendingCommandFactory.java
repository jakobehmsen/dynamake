package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public interface PendingCommandFactory<T> {
	T getReference();
	CommandState<T> createPendingCommand();
	void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector);
	Class<? extends TransactionHandler<T>> getTransactionHandlerClass();
	
	public static class Util {
		public static <T> void executeSingle(Collector<T> collector, final T reference, final Class<? extends TransactionHandler<T>> transactionHandlerClass, final CommandState<T> pendingCommand) {
			collector.execute(new SimplePendingCommandFactory<T>(reference, pendingCommand) {
				@Override
				public Class<? extends TransactionHandler<T>> getTransactionHandlerClass() {
					return transactionHandlerClass;
				}
			});
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands) {
			executeSequence(collector, reference, pendingCommands, new ExecutionsHandler<T>() {
				@Override
				public void handleExecutions(List<Execution<T>> executions, Collector<T> collector) { }
			});
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, Class<? extends TransactionHandler<T>> transactionHandlerClass) {
			executeSequence(collector, reference, pendingCommands, transactionHandlerClass, new ExecutionsHandler<T>() {
				@Override
				public void handleExecutions(List<Execution<T>> executions, Collector<T> collector) { }
			});
		}
		
		@SuppressWarnings("unchecked")
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, ExecutionsHandler<T> afterExecutions) {
			executeSequence(collector, reference, pendingCommands, (Class<? extends TransactionHandler<T>>)NullTransactionHandler.class, afterExecutions, new ArrayList<Execution<T>>(), 0);
		}
		
		public static <T> void executeSequence(Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, Class<? extends TransactionHandler<T>> transactionHandlerClass, ExecutionsHandler<T> afterExecutions) {
			executeSequence(collector, reference, pendingCommands, transactionHandlerClass, afterExecutions, new ArrayList<Execution<T>>(), 0);
		}
		
		private static <T> void executeSequence(
				Collector<T> collector, final T reference, final List<CommandState<T>> pendingCommands, final Class<? extends TransactionHandler<T>> transactionHandlerClass, 
				final ExecutionsHandler<T> afterExecutions, final List<Execution<T>> executions, final int i) {
			if(i < pendingCommands.size()) {
				collector.execute(new SimplePendingCommandFactory<T>(reference, pendingCommands.get(i)) {
					@Override
					public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
						executions.add(execution);
						executeSequence(collector, reference, pendingCommands, transactionHandlerClass, afterExecutions, executions, i + 1);
					}
					
					@Override
					public Class<? extends TransactionHandler<T>> getTransactionHandlerClass() {
						return transactionHandlerClass;
					}
				});
			} else {
				afterExecutions.handleExecutions(executions, collector);
			}
		}
	}
}
