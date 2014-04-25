package dynamake;

import java.util.Hashtable;

public class CreationModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Factory factory;
	private String[] parameterNames;

	public CreationModelFactory(Factory factory, String[] parameterNames) {
		this.factory = factory;
		this.parameterNames = parameterNames;
	}

	@Override
	public String getName() {
		return "Creation of Creation";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new CreationModel(factory, parameterNames);
	}
}
