package dynamake;

import java.awt.Rectangle;
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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance) {
		return new CreationModel(factory, parameterNames);
	}
}
