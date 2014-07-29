package dynamake.models.factories;


import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CloneDeepFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	
	public CloneDeepFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Model model = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		Model clone = model.cloneDeep();
		
		return new ModelCreation.Const(clone);
	}
}
