package dynamake.models.factories;


import java.awt.Rectangle;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.Primitive;
import dynamake.models.PropogationContext;
import dynamake.models.Primitive.Implementation;
import dynamake.numbers.Fraction;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;

public class PrimitiveSingletonFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Primitive.Implementation implementationSingleton;
	private Rectangle creationBounds;
	
	public PrimitiveSingletonFactory(Implementation implementationSingleton, Rectangle creationBounds) {
		this.implementationSingleton = implementationSingleton;
		this.creationBounds = creationBounds;
	}

	@Override
	public String getName() {
		return implementationSingleton.getName();
	}
	
	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Primitive model = new Primitive(implementationSingleton);
		Fraction fontSize = new Fraction(12);
		fontSize = fontSize.multiply(new Fraction(creationBounds.height, 35));
		IsolatingCollector<Model> isolatedCollector = new IsolatingCollector<Model>(collector);
		model.setProperty("FontSize", fontSize, propCtx, propDistance, isolatedCollector);
		return model;
	}
}
