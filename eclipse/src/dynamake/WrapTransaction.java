package dynamake;

import java.awt.Rectangle;
import java.util.Date;

import org.prevayler.Transaction;

public class WrapTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location liveModelLocation;
	private Location targetLocation;
	private Rectangle creationBounds;
	private Location[] modelLocations;
	
	public WrapTransaction(Location liveModelLocation, Location canvasLocation, Rectangle creationBounds, Location[] modelLocations) {
		this.liveModelLocation = liveModelLocation;
		this.targetLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection) {
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
		
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = new CanvasModel();
		
		wrapper.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, connection, branch);
		wrapper.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, connection, branch);
		wrapper.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, connection, branch);
		wrapper.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, connection, branch);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)modelLocations[i].getChild(prevalentSystem);
			
			models[i] = model;
		}
		
		for(Model model: models) {
			target.removeModel(model, propCtx, 0, connection);
			wrapper.addModel(model, propCtx, 0, connection);
		}
		
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");
			
			model.setProperty("X", x.subtract(new Fraction(creationBounds.x)), propCtx, 0, connection, branch);
			model.setProperty("Y", y.subtract(new Fraction(creationBounds.y)), propCtx, 0, connection, branch);
		}

		target.addModel(wrapper, propCtx, 0, connection);
		liveModel.setOutput(wrapper, propCtx, 0, connection);
	}
}
