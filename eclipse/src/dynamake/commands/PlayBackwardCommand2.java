package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.DualCommand;
import dynamake.transcription.Collector;

public class PlayBackwardCommand2 implements Command<Model> {
	public static class AfterPlayForward implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			PlayForwardCommand2.Output playForwardOutput = (PlayForwardCommand2.Output)output; 
			return new PlayBackwardCommand2(playForwardOutput.commands);
		}
	}

	public class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final List<DualCommand> commands;
		
		public Output(List<DualCommand> commands) {
			this.commands = commands;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Model.DualCommand> commands;

	public PlayBackwardCommand2(List<Model.DualCommand> commands) {
		this.commands = commands;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.playBackwards2(commands, propCtx, 0, collector);
		
		return null;
	}
}
