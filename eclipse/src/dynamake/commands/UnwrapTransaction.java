package dynamake.commands;

import java.awt.Rectangle;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.TranscriberCollector;

public class UnwrapTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location targetLocation;
	private Location wrapperLocationInTarget;
	private Rectangle creationBounds;
	
	public UnwrapTransaction(Location targetLocation, Location wrapperLocationInTarget, Rectangle creationBounds) {
		this.targetLocation = targetLocation;
		this.wrapperLocationInTarget = wrapperLocationInTarget;
		this.creationBounds = creationBounds;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = (CanvasModel)wrapperLocationInTarget.getChild(target);
		
		Model[] models = new Model[wrapper.getModelCount()];
		for(int i = 0; i <  wrapper.getModelCount(); i++) {
			Model model = wrapper.getModel(i);
			
			models[i] = model;
		}

		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			wrapper.removeModel(model, propCtx, 0, collector);
		}

		// Removed wrapper from target
		target.removeModel(wrapper, propCtx, 0, collector);

		// Offset the coordinates of the moved models
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");

			model.setProperty("X", x.add(new Fraction(creationBounds.x)), propCtx, 0, collector);
			model.setProperty("Y", y.add(new Fraction(creationBounds.y)), propCtx, 0, collector);
		}
		
		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			target.addModel(model, propCtx, 0, collector);
		}
	}
}
