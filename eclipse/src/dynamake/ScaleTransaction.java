package dynamake;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Date;

import org.prevayler.Transaction;

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
//		PropogationContext propCtx = new PropogationContext();
		Model model = (Model)modelLocation.getChild(prevalentSystem);
//		Model model = prevalentSystem;
		model.scale(newBounds, propCtx, 0, connection, branch);
		
//		int currentWidth = (int)model.getProperty("Width");
//		int currentHeight = (int)model.getProperty("Height");
//		
//		float widthChange = (float)newSize.width / currentWidth;
//		float heightChange = (float)newSize.height / currentHeight;
//		
//		model.scale(widthChange, heightChange, propCtx, 0);
	}

//	@Override
//	public Command<Model> antagonist() {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
