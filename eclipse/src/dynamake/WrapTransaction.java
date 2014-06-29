package dynamake;

import java.awt.Rectangle;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class WrapTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location targetLocation;
	private Rectangle creationBounds;
	private Location[] modelLocations;
	
	public WrapTransaction(Location canvasLocation, Rectangle creationBounds, Location[] modelLocations) {
		this.targetLocation = canvasLocation;
		this.creationBounds = creationBounds;
		this.modelLocations = modelLocations;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberBranch<Model> branch) {
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = new CanvasModel();
		
		TranscriberBranch<Model> propertyBranch = branch.isolatedBranch();
		
		wrapper.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, propertyBranch);
		wrapper.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, propertyBranch);
		wrapper.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, propertyBranch);
		wrapper.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, propertyBranch);
		
		Model[] models = new Model[modelLocations.length];
		for(int i = 0; i < modelLocations.length; i++) {
			Model model = (Model)modelLocations[i].getChild(prevalentSystem);
			
			models[i] = model;
		}
		
		for(Model model: models) {
			TranscriberBranch<Model> removeBranch = branch.branch();
			TranscriberBranch<Model> addBranch = branch.branch();
			
			target.removeModel(model, propCtx, 0, removeBranch);
			wrapper.addModel(model, propCtx, 0, addBranch);
			
			removeBranch.close();
			addBranch.close();
		}
		
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");

			TranscriberBranch<Model> setXBranch = branch.branch();
			TranscriberBranch<Model> setYBranch = branch.branch();
			
			model.setProperty("X", x.subtract(new Fraction(creationBounds.x)), propCtx, 0, setXBranch);
			model.setProperty("Y", y.subtract(new Fraction(creationBounds.y)), propCtx, 0, setYBranch);
			
			setXBranch.close();
			setYBranch.close();
		}

		target.addModel(wrapper, propCtx, 0, branch);
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
