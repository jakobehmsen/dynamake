package dynamake.models.factories;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;

public class CloneFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	
	public CloneFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Model model = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		RestorableModel restorableModel = RestorableModel.wrap(model, true);
		return restorableModel.unwrap(propCtx, propDistance, collector);
	}
	
	@Override
	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
