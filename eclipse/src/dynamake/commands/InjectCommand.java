package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class InjectCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location<Model> sourceLocation;
	private Location<Model> targetLocation;

	public InjectCommand(Location<Model> sourceLocation, Location<Model> targetLocation) {
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		Model source = CompositeLocation.getChild(prevalentSystem, location, sourceLocation);
		Model target = CompositeLocation.getChild(prevalentSystem, location, targetLocation);
		
		source.inject(target);
		
		// TODO: Consider whether a change should be sent out here
		return null;
	}
}
