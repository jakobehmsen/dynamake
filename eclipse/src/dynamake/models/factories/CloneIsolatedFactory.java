package dynamake.models.factories;

import java.awt.Rectangle;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CloneIsolatedFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	
	public CloneIsolatedFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "Close Isolated";
	}

	@Override
	public Model create(Model rootModel, Rectangle creationBounds, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		PropogationContext propCtx = new PropogationContext();
		
		Model model = (Model)modelLocation.getChild(rootModel);
		Model clone = model.cloneIsolated();
		
		return clone;
	}
}
