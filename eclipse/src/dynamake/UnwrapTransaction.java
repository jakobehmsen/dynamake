package dynamake;

import java.awt.Rectangle;
import java.util.Date;

public class UnwrapTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location liveModelLocation;
	private Location targetLocation;
	private Location wrapperLocation;
	private int[] modelIndexes;
	private Rectangle creationBounds;
	private Location outputLocation;
	
	public UnwrapTransaction(Location liveModelLocation, Location canvasLocation, Location wrapperLocation, int[] modelIndexes, Rectangle creationBounds, Location outputLocation) {
		this.liveModelLocation = liveModelLocation;
		this.targetLocation = canvasLocation;
		this.wrapperLocation = wrapperLocation;
		this.modelIndexes = modelIndexes;
		this.creationBounds = creationBounds;
		this.outputLocation = outputLocation;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection) {
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
		
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = (CanvasModel)wrapperLocation.getChild(target);
		
		Model[] models = new Model[wrapper.getModelCount()];
		for(int i = 0; i <  wrapper.getModelCount(); i++) {
			Model model = wrapper.getModel(i);
			
			models[i] = model;
		}

		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
//			int modelIndex = modelIndexes[i];
			wrapper.removeModel(model, propCtx, 0, connection);
//			target.addModel(modelIndex, model, propCtx, 0);
		}

		// Removed wrapper from target
		target.removeModel(wrapper, propCtx, 0, connection);

		// Offset the coordinates of the moved models
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");
			
			model.setProperty("X", x.add(new Fraction(creationBounds.x)), propCtx, 0, connection, branch);
			model.setProperty("Y", y.add(new Fraction(creationBounds.y)), propCtx, 0, connection, branch);
		}
		
		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			int modelIndex = modelIndexes[i];
//			wrapper.removeModel(model, propCtx, 0);
			target.addModel(modelIndex, model, propCtx, 0, connection);
		}
		
		// Set output
		if(outputLocation != null) {
			Model output = (Model)outputLocation.getChild(prevalentSystem);
			liveModel.setOutput(output, propCtx, 0, connection);
		} else {
			liveModel.setOutput(null, propCtx, 0, connection);
		}
	}
}
