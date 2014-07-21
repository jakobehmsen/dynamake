package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.IsolatingCollector;

public class RedoCommand implements Command<Model> {
	/*
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean isolate;

	public RedoCommand(boolean isolate) {
		this.isolate = isolate;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		if(isolate)
			collector = new IsolatingCollector<Model>(collector);
		
		model.redo(propCtx, 0, collector);
		
		return null;
	}
}