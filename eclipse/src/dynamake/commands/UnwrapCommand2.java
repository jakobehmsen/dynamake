package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class UnwrapCommand2 implements Command2<Model> {
	public static class AfterWrap implements Command2Factory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command2<Model> createCommand(Object output) {
			WrapCommand2.Output wrapOutput = (WrapCommand2.Output)output;
			return new UnwrapToLocationsCommand2(wrapOutput.wrapperLocationInTarget, wrapOutput.creationBounds);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location wrapperLocationInTarget;
	private RectangleF creationBounds;
	
	public UnwrapCommand2(Location wrapperLocationInTarget, RectangleF creationBounds) {
		this.wrapperLocationInTarget = wrapperLocationInTarget;
		this.creationBounds = creationBounds;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		CanvasModel target = (CanvasModel)location.getChild(prevalentSystem);
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

			model.setProperty("X", x.add(creationBounds.x), propCtx, 0, collector);
			model.setProperty("Y", y.add(creationBounds.y), propCtx, 0, collector);
		}
		
		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			target.addModel(model, propCtx, 0, collector);
		}
		
		Location[] modelLocations = new Location[models.length];
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			modelLocations[i] = target.getLocationOf(model);
		}
		
		return new UnwrapToLocationsCommand2.Output(creationBounds, modelLocations);
	}
}
