package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

public class TextModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Text";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance) {
		return new TextModel();
	}
}
