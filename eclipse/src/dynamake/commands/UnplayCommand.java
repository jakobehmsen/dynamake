package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnplayCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int count;

	public UnplayCommand(int count) {
		this.count = count;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model model = (Model)location.getChild(prevalentSystem);
//		System.out.println("Performing unplay on " + model + "...");
		
		model.unplay(count, propCtx, 0, collector);
		
//		System.out.println("Performed unplay on " + model);
		
		return null;
	}
}
