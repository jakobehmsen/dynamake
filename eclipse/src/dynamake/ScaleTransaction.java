package dynamake;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Date;

import org.prevayler.Transaction;

public class ScaleTransaction implements Transaction<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	private Dimension newSize;

	public ScaleTransaction(Location modelLocation, Dimension newSize) {
		this.modelLocation = modelLocation;
		this.newSize = newSize;
	}

	@Override
	public void executeOn(Model prevalentSystem, Date executionTime) {
		PropogationContext propCtx = new PropogationContext();
//		Model model = (Model)modelLocation.getChild(prevalentSystem);
		Model model = prevalentSystem;
		
		int currentWidth = (int)model.getProperty("Width");
		int currentHeight = (int)model.getProperty("Height");
		
		float widthChange = (float)newSize.width / currentWidth;
		float heightChange = (float)newSize.height / currentHeight;
		
		model.scale(widthChange, heightChange, propCtx, 0);
	}
}
