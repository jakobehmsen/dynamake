package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.TranscriberBranch;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class CloneDeepFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	
	public CloneDeepFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "Close Deep";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
//		PropogationContext propCtx = new PropogationContext();
		
		Model model = (Model)modelLocation.getChild(rootModel);
		Model clone = model.cloneDeep();
		
		return clone;
	}
}
