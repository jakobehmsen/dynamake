package dynamake.commands;

import java.util.Stack;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AppendLogCommand implements Command<Model> {
	public static class Output {
		public final Stack<CommandState<Model>> redoStack;

		public Output(Stack<CommandState<Model>> redoStack) {
			this.redoStack = redoStack;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CommandState<Model> change;

	public AppendLogCommand(CommandState<Model> change) {
		this.change = change;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		Stack<CommandState<Model>> redoStack = model.getRedoStack();
		model.appendLog(change, propCtx, 0, collector);
		
		return new Output(redoStack);
	}
}
