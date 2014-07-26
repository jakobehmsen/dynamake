package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.Model.DualCommand;
import dynamake.transcription.Collector;

public class PlayForwardCommand2 implements Command<Model> {
	public static class AfterPlayBackward implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			PlayBackwardCommand2.Output playBackwardOutput = (PlayBackwardCommand2.Output)output;
			
			return new PlayForwardCommand2(playBackwardOutput.commands);
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

	public PlayForwardCommand2(List<Model.DualCommand> commands) {
		this.commands = commands;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.playForwards2(commands, propCtx, 0, collector);
		
		return null;
	}
}
