package dynamake;

import java.util.Hashtable;

import dynamake.Primitive.Implementation;

public class PrimitiveSingletonFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Primitive.Implementation implementationSingleton;
	
	public PrimitiveSingletonFactory(Implementation implementationSingleton) {
		this.implementationSingleton = implementationSingleton;
	}

	@Override
	public String getName() {
		return implementationSingleton.getName();
	}
	
	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new Primitive(implementationSingleton);
	}
}
