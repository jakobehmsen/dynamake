package dynamake;

import java.util.Hashtable;

public class BackgroundSetterFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Background Setter";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new BackgroundSetter();
	}
}
