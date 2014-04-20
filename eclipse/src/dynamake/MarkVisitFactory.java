package dynamake;

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
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model model = (Model)modelLocation.getChild(rootModel);
		
		return new MarkVisit(model);
	}
}
