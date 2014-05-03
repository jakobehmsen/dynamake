package dynamake;

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
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		String text = (String)arguments.get("Text");
		if(text == null)
			text = "Button";
		return new ButtonModel(text);
	}
}
