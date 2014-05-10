package dynamake;

import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Transaction;

public class AddThenSelectTransaction implements Transaction<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location liveModelLocation;
	private Location canvasLocation;
	private Factory factory;
	private Rectangle creationBounds;

	public AddThenSelectTransaction(Location liveModelLocation, Location canvasLocation, Rectangle creationBounds, Factory factory) {
		this.liveModelLocation = liveModelLocation;
		this.canvasLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.factory = factory;
	}

	@Override
	public void executeOn(Model rootPrevalentSystem, Date executionTime) {
		PropogationContext propCtx = new PropogationContext();
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(rootPrevalentSystem);
		CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
		Model model = (Model)factory.create(rootPrevalentSystem, new Hashtable<String, Object>());

		model.setProperty("X", creationBounds.x, propCtx, 0);
		model.setProperty("Y", creationBounds.y, propCtx, 0);
		model.setProperty("Width", creationBounds.width, propCtx, 0);
		model.setProperty("Height", creationBounds.height, propCtx, 0);
		
		canvas.addModel(model, new PropogationContext(), 0);
		
		liveModel.setSelection(model, propCtx, 0);
	}
}
