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
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		Model source = (Model)sourceLocation.getChild(prevalentSystem);
		Model target = (Model)targetLocation.getChild(prevalentSystem);
		
		source.inject(target);
	}
}
