package dynamake;

import java.awt.Rectangle;
import java.util.Date;

import org.prevayler.Transaction;

public class WrapTransaction implements Transaction<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location targetLocation;
	private Rectangle creationBounds;
	private Location[] modelLocations;
	
	public WrapTransaction(Location canvasLocation, Rectangle creationBounds, Location[] modelLocations) {
		this.targetLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public void executeOn(Model arg0, Date arg1) {
		PropogationContext propCtx = new PropogationContext();
		
		CanvasModel target = (CanvasModel)targetLocation.getChild(arg0);
		CanvasModel wrapper = new CanvasModel();
		
		wrapper.setProperty("X", creationBounds.x, propCtx, 0);
		wrapper.setProperty("Y", creationBounds.y, propCtx, 0);
		wrapper.setProperty("Width", creationBounds.width, propCtx, 0);
		wrapper.setProperty("Height", creationBounds.height, propCtx, 0);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)modelLocations[i].getChild(arg0);
			
			int x = (int)model.getProperty("X");
			int y = (int)model.getProperty("Y");
			
			model.setProperty("X", x - creationBounds.x, propCtx, 0);
			model.setProperty("Y", y - creationBounds.y, propCtx, 0);
			
			models[i] = model;
		}
		
		for(Model model: models) {
			target.removeModel(model, propCtx, 0);
			wrapper.addModel(model, propCtx, 0);
		}
		
		target.addModel(wrapper, propCtx, 0);
	}
}
