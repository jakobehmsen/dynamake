package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class SetPropertyCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model model = (Model)location.getChild(prevalentSystem);

		Object value = scope.consume();
		String name = (String)scope.consume();
		
		Object previousValue = model.getProperty(name);
		model.setProperty(name, value, propCtx, 0, collector);
//		System.out.println("Set property " + name + " to " + value);
		
		scope.produce(name);
		scope.produce(previousValue);
		
		return null;
	}
}
