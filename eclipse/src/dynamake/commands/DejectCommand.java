package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class DejectCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location sourceLocation;
	private Location targetLocation;

	public DejectCommand(Location sourceLocation, Location targetLocation) {
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model source = (Model)sourceLocation.getChild(prevalentSystem);
		Model target = (Model)targetLocation.getChild(prevalentSystem);
		
		source.deject(target);
		
		// TODO: Consider whether a change should be sent out here
		return null;
	}
}
