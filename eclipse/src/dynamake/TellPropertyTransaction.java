package dynamake;

import java.util.Date;

public class TellPropertyTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location modelLocation;
	private String propertyName;
	
	public TellPropertyTransaction(Location modelLocation, String propertyName) {
		this.modelLocation = modelLocation;
		this.propertyName = propertyName;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
		Model receiver = (Model)modelLocation.getChild(prevalentSystem);
		receiver.changed(null, new Model.TellProperty(propertyName), propCtx, 0, 1, branch);
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
