package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PlayThenReverseCommand implements Command<Model> {
	public static class AfterPlay implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			PlayThenReverseCommand.Output playOutput = (PlayThenReverseCommand.Output)output;
			
			return new PlayThenReverseCommand(playOutput.commands);
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

	public PlayThenReverseCommand(List<CommandState<Model>> commands) {
		this.commands = commands;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		System.out.println("Performing play on " + model + "...");
		
		List<CommandState<Model>> revertibles = model.playThenReverse(commands, propCtx, 0, collector);
		
		System.out.println("Performed play on " + model);
		
		return new Output(revertibles);
	}
}
