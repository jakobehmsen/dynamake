package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CreateAndExecuteFromProperty implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private CommandFactory<Model> commandFactory;

	public CreateAndExecuteFromProperty(String name, CommandFactory<Model> commandFactory) {
		this.name = name;
		this.commandFactory = commandFactory;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		Object value = model.getProperty(name);
		Command<Model> command = commandFactory.createCommand(value);

		return command.executeOn(propCtx, prevalentSystem, collector, location);
	}
}
