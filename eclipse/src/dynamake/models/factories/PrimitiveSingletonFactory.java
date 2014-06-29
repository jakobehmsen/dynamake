package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.Fraction;
import dynamake.models.Model;
import dynamake.models.Primitive;
import dynamake.models.PropogationContext;
import dynamake.models.Primitive.Implementation;
import dynamake.transcription.TranscriberBranch;

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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		Primitive model = new Primitive(implementationSingleton);
		Fraction fontSize = new Fraction(12);
		fontSize = fontSize.multiply(new Fraction(creationBounds.height, 35));
		TranscriberBranch<Model> setPropertyBranch = branch.isolatedBranch();
//		Fraction fontSize = 12 * 40 creationBounds.height;
		model.setProperty("FontSize", fontSize, propCtx, propDistance, setPropertyBranch);
		return model;
		
//		return new Primitive(implementationSingleton);
	}
}
