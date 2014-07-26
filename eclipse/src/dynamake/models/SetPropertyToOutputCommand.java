package dynamake.models;

import dynamake.commands.Command;
import dynamake.transcription.Collector;

public class SetPropertyToOutputCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private Command<Model> command;

	public SetPropertyToOutputCommand(String name, Command<Model> command) {
		this.name = name;
		this.command = command;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		Object output = command.executeOn(propCtx, prevalentSystem, collector, location);
		model.setProperty(name, output, propCtx, 0, collector);
		
		return output;
	}
}
