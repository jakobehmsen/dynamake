package dynamake;

import java.util.Hashtable;

public class BackgroundGetterFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Background Getter";
	}

	@Override
	public Object create(Hashtable<String, Object> arguments) {
		return new BackgroundGetter();
	}
}
