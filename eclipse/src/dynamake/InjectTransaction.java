package dynamake;

import java.util.Date;

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
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
		Model source = (Model)sourceLocation.getChild(prevalentSystem);
		Model target = (Model)targetLocation.getChild(prevalentSystem);
		
		source.inject(target);
		
		PrevaylerServiceBranch<Model> innerBranch = branch.branch();
		innerBranch.close();
//		innerBranch.absorb();
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
