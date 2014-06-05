package dynamake;

import java.awt.Rectangle;
import java.util.Date;

public class ScaleTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	private Rectangle newBounds;

	public ScaleTransaction(Location modelLocation, Rectangle newBounds) {
		this.modelLocation = modelLocation;
		this.newBounds = newBounds;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		Model model = (Model)modelLocation.getChild(prevalentSystem);
		model.scale(newBounds, propCtx, 0, connection, branch);
	}
}
