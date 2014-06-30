package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;

public class InjectTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location sourceLocation;
	private Location targetLocation;

	public InjectTransaction(Location sourceLocation, Location targetLocation) {
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberBranch<Model> branch) {
		Model source = (Model)sourceLocation.getChild(prevalentSystem);
		Model target = (Model)targetLocation.getChild(prevalentSystem);
		
		source.inject(target);
		
		// TODO: Consider whether a change should be sent out here
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}