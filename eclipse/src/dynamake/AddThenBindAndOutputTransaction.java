package dynamake;

import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Transaction;

public class AddThenBindAndOutputTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location liveModelLocation;
	private Location modelToBindToLocation;
	private Location canvasModelLocation;
	private Factory factory;
	private Rectangle creationBounds;

	public AddThenBindAndOutputTransaction(Location liveModelLocation, Location modelToBindToLocation,
			Location canvasModelLocation, Factory factory,
			Rectangle creationBounds) {
		this.liveModelLocation = liveModelLocation;
		this.modelToBindToLocation = modelToBindToLocation;
		this.canvasModelLocation = canvasModelLocation;
		this.factory = factory;
		this.creationBounds = creationBounds;
	}

	@Override
	public void executeOn(Model prevalentSystem, Date executionTime) {
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
		final Model modelToBindTo = (Model)modelToBindToLocation.getChild(prevalentSystem);
		CanvasModel canvasModel = (CanvasModel)canvasModelLocation.getChild(prevalentSystem);
		
		final PropogationContext addAndBindCtx = new PropogationContext();
		final Model primitive = (Model)factory.create(prevalentSystem, creationBounds, new Hashtable<String, Object>(), addAndBindCtx, 0);
		primitive.setProperty("X", new Fraction(creationBounds.x), addAndBindCtx, 0);
		primitive.setProperty("Y", new Fraction(creationBounds.y), addAndBindCtx, 0);
		primitive.setProperty("Width", new Fraction(creationBounds.width), addAndBindCtx, 0);
		primitive.setProperty("Height", new Fraction(creationBounds.height), addAndBindCtx, 0);
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

		liveModel.setOutput(primitive, addAndBindCtx, 0);
		canvasModel.addModel(primitive, addAndBindCtx, 0);
	}

	@Override
	public Command<Model> antagonist() {
		// TODO Auto-generated method stub
		return null;
	}
}
