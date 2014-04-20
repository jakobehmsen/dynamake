package dynamake;

import java.util.Hashtable;

public class NotVisitedFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;

	public NotVisitedFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "Not Visited";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model model = (Model)modelLocation.getChild(rootModel);
		
		return new NotVisited(model);
	}
}
