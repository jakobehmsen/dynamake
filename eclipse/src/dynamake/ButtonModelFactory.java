package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

public class ButtonModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Button";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance) {
		String text = (String)arguments.get("Text");
		if(text == null)
			text = "Button";
		return new ButtonModel(text);
	}
}
