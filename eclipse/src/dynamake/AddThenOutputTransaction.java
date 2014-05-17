package dynamake;

import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Transaction;

public class AddThenOutputTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location liveModelLocation;
	private Location canvasLocation;
	private Factory factory;
	private Rectangle creationBounds;
	private Hashtable<String, Object> arguments;

	public AddThenOutputTransaction(Location liveModelLocation, Location canvasLocation, Rectangle creationBounds, Factory factory) {
		this.liveModelLocation = liveModelLocation;
		this.canvasLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.factory = factory;
		arguments = new Hashtable<String, Object>();
	}

	public AddThenOutputTransaction(Location liveModelLocation, Location canvasLocation, Rectangle creationBounds, Factory factory, Hashtable<String, Object> arguments) {
		this.liveModelLocation = liveModelLocation;
		this.canvasLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.factory = factory;
		this.arguments = arguments;
	}

	@Override
	public void executeOn(Model rootPrevalentSystem, Date executionTime) {
		PropogationContext propCtx = new PropogationContext();
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(rootPrevalentSystem);
		CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
		Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, arguments, propCtx, 0);

		model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0);
		model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0);
		model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0);
		model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0);
		
		liveModel.setOutput(model, propCtx, 0);
		canvas.addModel(model, propCtx, 0);
	}
}
