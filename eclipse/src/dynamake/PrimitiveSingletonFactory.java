package dynamake;

import java.awt.Rectangle;
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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance) {
		return new Primitive(implementationSingleton);
	}
}
