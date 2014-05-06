package dynamake;

import java.util.Hashtable;

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
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		PropogationContext propCtx = new PropogationContext();
		
		Model model = (Model)modelLocation.getChild(rootModel);
		Model clone = model.cloneDeep();
		
		return clone;
	}
}
