package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnplayCommand2 implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int count;

	public UnplayCommand2(int count) {
		this.count = count;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.unplay2(count, propCtx, 0, collector);
		
		return null;
	}
}
