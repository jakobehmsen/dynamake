package dynamake.commands;

import java.awt.Rectangle;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;

public class WrapCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location targetLocation;
	private Rectangle creationBounds;
	private Location[] modelLocations;
	
	public WrapCommand(Location canvasLocation, Rectangle creationBounds, Location[] modelLocations) {
		this.targetLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = new CanvasModel();
		
		IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
		
		wrapper.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, isolatedCollector);
		wrapper.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, isolatedCollector);
		wrapper.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, isolatedCollector);
		wrapper.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, isolatedCollector);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)modelLocations[i].getChild(prevalentSystem);
			
			models[i] = model;
		}
		
		for(Model model: models) {
			target.removeModel(model, propCtx, 0, collector);
			wrapper.addModel(model, propCtx, 0, collector);
		}
		
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");
			
			model.setProperty("X", x.subtract(new Fraction(creationBounds.x)), propCtx, 0, collector);
			model.setProperty("Y", y.subtract(new Fraction(creationBounds.y)), propCtx, 0, collector);
		}

		target.addModel(wrapper, propCtx, 0, collector);
	}
}
