package dynamake.models;

import java.util.List;

import dynamake.commands.Command;
import dynamake.transcription.Collector;

public class PlayForwardCommand2 implements Command<Model> {
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
