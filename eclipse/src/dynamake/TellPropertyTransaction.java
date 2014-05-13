package dynamake;

import java.util.Date;

import org.prevayler.Transaction;

public class TellPropertyTransaction implements Transaction<Model> {
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
	public void executeOn(Model prevalentSystem, Date executionTime) {
		PropogationContext propCtx = new PropogationContext();
		
		Model receiver = (Model)modelLocation.getChild(prevalentSystem);
		receiver.changed(null, new Model.TellProperty(propertyName), propCtx, 0, 1);
	}
}