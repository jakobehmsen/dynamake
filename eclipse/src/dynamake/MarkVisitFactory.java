package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

public class MarkVisitFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;

	public MarkVisitFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "Mark Visit";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		Model model = (Model)modelLocation.getChild(rootModel);
		
		return new MarkVisit(model);
	}
}
