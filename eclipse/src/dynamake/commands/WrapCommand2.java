package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;

public class WrapCommand2 implements Command2<Model> {
	public static class AfterUnwrap implements Command2Factory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command2<Model> createCommand(Object output) {
			UnwrapToLocationsCommand2.Output unwrapOutput = (UnwrapToLocationsCommand2.Output)output;
			return new WrapCommand2(unwrapOutput.creationBounds, unwrapOutput.modelLocations);
		}
	}
	
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location wrapperLocationInTarget;
		public final RectangleF creationBounds;

		public Output(Location wrapperLocationInTarget, RectangleF creationBounds) {
			this.wrapperLocationInTarget = wrapperLocationInTarget;
			this.creationBounds = creationBounds;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location[] modelLocations;
	
	public WrapCommand2(RectangleF creationBounds, Location[] modelLocations) {
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		CanvasModel target = (CanvasModel)location.getChild(prevalentSystem);
		CanvasModel wrapper = new CanvasModel();
		
		IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
		
		wrapper.setBounds(creationBounds, propCtx, 0, isolatedCollector);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)CompositeModelLocation.getChild(prevalentSystem, location, modelLocations[i]);
//			Model model = (Model)modelLocations[i].getChild(prevalentSystem);
			
			models[i] = model;
		}

		for(Model model: models) {
			target.removeModel(model, propCtx, 0, collector);
			wrapper.addModel(model, propCtx, 0, collector);
		}
		
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");
			
			model.setProperty("X", x.subtract(creationBounds.x), propCtx, 0, collector);
			model.setProperty("Y", y.subtract(creationBounds.y), propCtx, 0, collector);
		}

		target.addModel(wrapper, propCtx, 0, collector);
		Location wrapperLocationInTarget = target.getLocationOf(wrapper);
		
		return new Output(wrapperLocationInTarget, creationBounds);
	}
}
