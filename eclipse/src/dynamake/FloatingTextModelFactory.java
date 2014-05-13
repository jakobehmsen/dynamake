package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

public class FloatingTextModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "FText";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance) {
		FloatingTextModel model = new FloatingTextModel();
		Fraction fontSize = new Fraction(12);
		fontSize = fontSize.multiply(new Fraction(creationBounds.height, 20));
//		Fraction fontSize = 12 * 40 creationBounds.height;
		model.setProperty("FontSize", fontSize, propCtx, propDistance);
		return model;
	}
}