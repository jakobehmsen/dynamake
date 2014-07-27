package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PlayCommand implements Command<Model> {
	public static class AfterPlay implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			PlayCommand.Output playOutput = (PlayCommand.Output)output;
			
			return new PlayCommand(playOutput.commands);
		}
	}
	
	public class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final List<CommandState<Model>> commands;
		
		public Output(List<CommandState<Model>> commands) {
			this.commands = commands;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<CommandState<Model>> commands;

	public PlayCommand(List<CommandState<Model>> commands) {
		this.commands = commands;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		List<CommandState<Model>> revertibles = model.play(commands, propCtx, 0, collector);
		
		return new Output(revertibles);
	}
}
