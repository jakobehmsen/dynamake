package dynamake;

import java.util.Hashtable;

public class CreateAndBindFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Factory factory;
	private Location locationOfModelToBindTo;

	public CreateAndBindFactory(Factory factory,
			Location locationOfModelToBindTo) {
		this.factory = factory;
		this.locationOfModelToBindTo = locationOfModelToBindTo;
	}

	@Override
	public String getName() {
		return factory.getName();
	}
	
	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model createdModel = (Model)factory.create(rootModel, arguments);
		Model modelToBindTo = (Model)locationOfModelToBindTo.getChild(rootModel);
		modelToBindTo.addObserver(createdModel);
		return createdModel;
	}
}
