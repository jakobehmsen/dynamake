package dynamake.commands;

import java.util.Stack;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RemoveLastLogCommand implements Command<Model> {
	public static class AfterAppendLog implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			return new RemoveLastLogCommand(((AppendLogCommand.Output)output).redoStack);
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Stack<CommandState<Model>> redoStack;

	public RemoveLastLogCommand(Stack<CommandState<Model>> redoStack) {
		this.redoStack = redoStack;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.removeLastLog(propCtx, 0, collector);
		model.restoreRedoStack(redoStack);
		
		return null;
	}
}
