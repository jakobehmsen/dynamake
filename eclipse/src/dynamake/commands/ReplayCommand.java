package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ReplayCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int count;

	public ReplayCommand(int count) {
		this.count = count;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model model = (Model)location.getChild(prevalentSystem);
//		System.out.println("Performing replay on " + model + "...");
		
		model.replay(count, propCtx, 0, collector, scope);
		
//		System.out.println("Performed replay on " + model);
		
		return null;
	}
}
