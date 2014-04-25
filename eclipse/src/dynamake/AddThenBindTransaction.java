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
		primitive.getMetaModel().set("X", creationBounds.x, addAndBindCtx);
		primitive.getMetaModel().set("Y", creationBounds.y, addAndBindCtx);
		primitive.getMetaModel().set("Width", creationBounds.width, addAndBindCtx);
		primitive.getMetaModel().set("Height", creationBounds.height, addAndBindCtx);
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
		
		canvasModel.addModel(primitive, addAndBindCtx);
	}
}