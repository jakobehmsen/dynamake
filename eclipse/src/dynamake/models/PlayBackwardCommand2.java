package dynamake.models;

import java.util.List;

import dynamake.commands.Command;
import dynamake.transcription.Collector;

public class PlayBackwardCommand2 implements Command<Model> {
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
