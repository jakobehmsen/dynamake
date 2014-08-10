package dynamake.commands;

import java.io.Serializable;
import java.util.Hashtable;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class UnwrapCommand implements Command<Model> {
	public static class AfterWrap implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			WrapCommand.Output wrapOutput = (WrapCommand.Output)output;
			return new UnwrapCommand(wrapOutput.wrapperLocationInTarget, wrapOutput.creationBounds, wrapOutput.wrapperToSourceLocations);
		}
	}
	
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location wrapperLocation;
		public final RectangleF creationBounds;
		public final Location[] modelLocations;

		public Output(Location wrapperLocation, RectangleF creationBounds, Location[] modelLocations) {
			this.wrapperLocation = wrapperLocation;
			this.creationBounds = creationBounds;
			this.modelLocations = modelLocations;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location wrapperLocationInTarget;
	private RectangleF creationBounds;
	private Hashtable<Location, Location> wrapperToSourceLocations;
	
	public UnwrapCommand(Location wrapperLocationInTarget, RectangleF creationBounds) {
		this(wrapperLocationInTarget, creationBounds, new Hashtable<Location, Location>());
	}
	
	public UnwrapCommand(Location wrapperLocationInTarget, RectangleF creationBounds, Hashtable<Location, Location> wrapperToSourceLocations) {
		this.wrapperLocationInTarget = wrapperLocationInTarget;
		this.creationBounds = creationBounds;
		this.wrapperToSourceLocations = wrapperToSourceLocations;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		CanvasModel target = (CanvasModel)location.getChild(prevalentSystem);
		CanvasModel wrapper = (CanvasModel)wrapperLocationInTarget.getChild(target);
		
		Location[] locationsInWrapper = wrapper.getLocations();
		Model[] models = new Model[wrapper.getModelCount()];
		for(int i = 0; i <  locationsInWrapper.length; i++) {
			Model model = wrapper.getModelByLocation(locationsInWrapper[i]);
			
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
			Location locationInWrapper = locationsInWrapper[i];
			Location locationInSource = wrapperToSourceLocations.get(locationInWrapper);
			// If wrapped model was part of a previous wrapping
			if(locationInSource != null)
				// then restore id
				target.restoreModelByLocation(locationInSource, model, propCtx, 0, collector);
			else
				// otherwise, it is new and should be given a new location
				target.addModel(model, propCtx, 0, collector);
		}
		
		Location[] modelLocations = new Location[models.length];
		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			modelLocations[i] = target.getLocationOf(model);
		}
		
		return new Output(wrapperLocationInTarget, creationBounds, modelLocations);
	}
}