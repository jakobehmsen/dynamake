package dynamake;

import java.util.Hashtable;

public class MetaModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;

	public MetaModelFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "Meta Model";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model model = (Model)modelLocation.getChild(rootModel);
		return model.getMetaModel();
	}
}
