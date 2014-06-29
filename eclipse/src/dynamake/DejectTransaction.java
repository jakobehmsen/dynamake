package dynamake;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class DejectTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location sourceLocation;
	private Location targetLocation;

	public DejectTransaction(Location sourceLocation, Location targetLocation) {
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
		Model source = (Model)sourceLocation.getChild(prevalentSystem);
		Model target = (Model)targetLocation.getChild(prevalentSystem);
		
		source.deject(target);
		
		// TODO: Consider whether a change should be sent out here
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
