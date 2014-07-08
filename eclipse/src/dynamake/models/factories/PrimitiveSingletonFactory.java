package dynamake.models.factories;

import java.awt.Rectangle;

import dynamake.models.Model;
import dynamake.models.Primitive;
import dynamake.models.PropogationContext;
import dynamake.models.Primitive.Implementation;
import dynamake.numbers.Fraction;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.TranscriberCollector;

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
	public Model create(Model rootModel, Rectangle creationBounds, PropogationContext propCtx, int propDistance, TranscriberCollector<Model> collector) {
		Primitive model = new Primitive(implementationSingleton);
		Fraction fontSize = new Fraction(12);
		fontSize = fontSize.multiply(new Fraction(creationBounds.height, 35));
//		TranscriberBranch<Model> setPropertyBranch = branch.isolatedBranch();
		IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<>(collector);
//		Fraction fontSize = 12 * 40 creationBounds.height;
		model.setProperty("FontSize", fontSize, propCtx, propDistance, null, isolatedCollector);
		return model;
		
//		return new Primitive(implementationSingleton);
	}
}
