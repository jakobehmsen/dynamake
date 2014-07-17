package dynamake.commands;

import java.util.Date;

import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class InjectCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location sourceLocation;
	private Location targetLocation;

	public InjectCommand(Location sourceLocation, Location targetLocation) {
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		Model source = (Model)CompositeModelLocation.getChild(prevalentSystem, location, sourceLocation);
		Model target = (Model)CompositeModelLocation.getChild(prevalentSystem, location, targetLocation);
		
		source.inject(target);
		
		// TODO: Consider whether a change should be sent out here
		return null;
	}
}
