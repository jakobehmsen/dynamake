package dynamake;

import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Transaction;

public class AddThenBindTransaction implements Transaction<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location modelToBindToLocation;
	private Location canvasModelLocation;
	private Factory factory;
	private Rectangle creationBounds;

	public AddThenBindTransaction(Location modelToBindToLocation,
			Location canvasModelLocation, Factory factory,
			Rectangle creationBounds) {
		this.modelToBindToLocation = modelToBindToLocation;
		this.canvasModelLocation = canvasModelLocation;
		this.factory = factory;
		this.creationBounds = creationBounds;
	}

	@Override
	public void executeOn(Model prevalentSystem, Date executionTime) {
		final Model modelToBindTo = (Model)modelToBindToLocation.getChild(prevalentSystem);
		CanvasModel canvasModel = (CanvasModel)canvasModelLocation.getChild(prevalentSystem);
		
		final PropogationContext addAndBindCtx = new PropogationContext();
		final Model primitive = (Model)factory.create(prevalentSystem, new Hashtable<String, Object>());
		primitive.setProperty("X", creationBounds.x, addAndBindCtx, 0);
		primitive.setProperty("Y", creationBounds.y, addAndBindCtx, 0);
		primitive.setProperty("Width", creationBounds.width, addAndBindCtx, 0);
		primitive.setProperty("Height", creationBounds.height, addAndBindCtx, 0);
		modelToBindTo.addObserver(primitive);

//		canvasModel.addObserver(new Observer() {
//			@Override
//			public void changed(Model sender, Object change, PropogationContext propCtx) {
//				if(propCtx.isOrDerivesFrom(addAndBindCtx)) {
//					sender.removeObserver(this);
//					
//					modelToBindTo.addObserver(primitive);
//				}
//			}
//		});
		
		canvasModel.addModel(primitive, addAndBindCtx, 0);
	}
}
