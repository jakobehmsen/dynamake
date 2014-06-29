package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
//		PropogationContext propCtx = new PropogationContext();
		
		Model model = (Model)modelLocation.getChild(rootModel);
		Model clone = model.cloneIsolated();
		
		return clone;
	}
}
