package dynamake;

import java.awt.Rectangle;
import java.util.Date;

import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class UnwrapTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location targetLocation;
	private Location wrapperLocationInTarget;
	private int[] modelIndexes;
	private Rectangle creationBounds;
	
	public UnwrapTransaction(Location targetLocation, Location wrapperLocationInTarget, int[] modelIndexes, Rectangle creationBounds) {
		this.targetLocation = targetLocation;
		this.wrapperLocationInTarget = wrapperLocationInTarget;
		this.modelIndexes = modelIndexes;
		this.creationBounds = creationBounds;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
		CanvasModel target = (CanvasModel)targetLocation.getChild(prevalentSystem);
		CanvasModel wrapper = (CanvasModel)wrapperLocationInTarget.getChild(target);
		
		Model[] models = new Model[wrapper.getModelCount()];
		for(int i = 0; i <  wrapper.getModelCount(); i++) {
			Model model = wrapper.getModel(i);
			
			models[i] = model;
		}

		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			PrevaylerServiceBranch<Model> removeBranch = branch.branch();
			
			Model model = models[i];
			wrapper.removeModel(model, propCtx, 0, removeBranch);
			
			removeBranch.close();
		}

		// Removed wrapper from target
		target.removeModel(wrapper, propCtx, 0, branch);

		// Offset the coordinates of the moved models
		for(Model model: models) {
			Fraction x = (Fraction)model.getProperty("X");
			Fraction y = (Fraction)model.getProperty("Y");

			PrevaylerServiceBranch<Model> setXBranch = branch.branch();
			PrevaylerServiceBranch<Model> setYBranch = branch.branch();
			
			model.setProperty("X", x.add(new Fraction(creationBounds.x)), propCtx, 0, setXBranch);
			model.setProperty("Y", y.add(new Fraction(creationBounds.y)), propCtx, 0, setYBranch);
			
			setXBranch.close();
			setYBranch.close();
		}
		
		// Move models from wrapper to target
		for(int i = 0; i < models.length; i++) {
			PrevaylerServiceBranch<Model> addBranch = branch.branch();
			
			Model model = models[i];
			int modelIndex = modelIndexes[i];
			target.addModel(modelIndex, model, propCtx, 0, addBranch);
			
			addBranch.close();
		}
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
