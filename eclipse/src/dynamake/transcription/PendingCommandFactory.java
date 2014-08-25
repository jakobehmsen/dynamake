package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.PropogationContext;

public interface PendingCommandFactory<T> {
	CommandState<T> createPendingCommand();
	void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector);
	
	public static class Util {
		public static <T> void executeSingle(Collector<T> collector, final CommandState<T> pendingCommand) {
			collector.execute(new SimplePendingCommandFactory<T>(pendingCommand));
		}
		
		public static <T> void executeSequence(Collector<T> collector, final List<CommandState<T>> pendingCommands) {
			executeSequence(collector, pendingCommands, new ExecutionsHandler<T>() {
				@Override
				public void handleExecutions(List<Execution<T>> executions, Collector<T> collector) { }
			});
		}
		
		public static <T> void executeSequence(Collector<T> collector, final List<CommandState<T>> pendingCommands, ExecutionsHandler<T> afterExecutions) {
			executeSequence(collector, pendingCommands, afterExecutions, new ArrayList<Execution<T>>(), 0);
		}
		
		private static <T> void executeSequence(
				Collector<T> collector, final List<CommandState<T>> pendingCommands, final ExecutionsHandler<T> afterExecutions, final List<Execution<T>> executions, 
				final int i) {
			if(i < pendingCommands.size()) {
				collector.execute(new SimplePendingCommandFactory<T>(pendingCommands.get(i)) {
					@Override
					public void afterPropogationFinished(Execution<T> execution, PropogationContext propCtx, int propDistance, Collector<T> collector) {
						executions.add(execution);
						executeSequence(collector, pendingCommands, afterExecutions, executions, i + 1);
					}
				});
			} else {
				afterExecutions.handleExecutions(executions, collector);
			}
		}
	}
}
