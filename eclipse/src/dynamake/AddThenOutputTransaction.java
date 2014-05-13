package dynamake;

import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Transaction;

public class AddThenOutputTransaction implements Transaction<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location liveModelLocation;
	private Location canvasLocation;
	private Factory factory;
	private Rectangle creationBounds;

	public AddThenOutputTransaction(Location liveModelLocation, Location canvasLocation, Rectangle creationBounds, Factory factory) {
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

		model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0);
		model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0);
		model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0);
		model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0);
		
		liveModel.setOutput(model, propCtx, 0);
		canvas.addModel(model, propCtx, 0);
	}
}
