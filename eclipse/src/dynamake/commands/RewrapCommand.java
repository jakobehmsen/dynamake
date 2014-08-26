package dynamake.commands;

import java.util.Hashtable;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;

public class RewrapCommand implements Command<Model> {
	public static class AfterUnwrap implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			UnwrapCommand.Output unwrapOutput = (UnwrapCommand.Output)output;
			return new RewrapCommand(unwrapOutput.wrapperLocation, unwrapOutput.creationBounds, unwrapOutput.modelLocations);
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location wrapperLocation;
	private RectangleF creationBounds;
	private Location[] modelLocations;
	
	public RewrapCommand(Location wrapperLocation, RectangleF creationBounds, Location[] modelLocations) {
		this.wrapperLocation = wrapperLocation;
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		CanvasModel target = (CanvasModel)location.getChild(prevalentSystem);
		// TODO: Consider: How to restore the history?
		// Should the actual wrapper be part of the re-wrap command? Or perhaps, just a clone of it? Or some sort of memento of it?
		CanvasModel wrapper = new CanvasModel();
		
		IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
		
		wrapper.setBounds(creationBounds, propCtx, 0, isolatedCollector);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)CompositeLocation.getChild(prevalentSystem, location, modelLocations[i]);
			
			models[i] = model;
		}
		
		Hashtable<Location, Location> wrapperToSourceLocations = new Hashtable<Location, Location>();

		for(int i = 0; i < models.length; i++) {
			Model model = models[i];
			target.removeModel(model, propCtx, 0, collector);
			wrapper.addModel(model, propCtx, 0, collector);

			// Map locations between source and wrapper to remember which models were wrapped during this wrap
			Location modelLocationInWrapper = wrapper.getLocationOf(model);
			Location modelLocationInSource = modelLocations[i];
			
			wrapperToSourceLocations.put(modelLocationInWrapper, modelLocationInSource);
		}
		
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");
			
			model.setProperty("X", x.subtract(creationBounds.x), propCtx, 0, collector);
			model.setProperty("Y", y.subtract(creationBounds.y), propCtx, 0, collector);
		}

		target.restoreModelByLocation(wrapperLocation, wrapper, propCtx, 0, collector);
		Location wrapperLocationInTarget = target.getLocationOf(wrapper);
		
		return new WrapCommand.Output(wrapperLocationInTarget, creationBounds, wrapperToSourceLocations);
	}
}
