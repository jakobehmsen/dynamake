package dynamake;

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
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new TextModel();
	}
}
