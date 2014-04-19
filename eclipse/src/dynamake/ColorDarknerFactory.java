package dynamake;

import java.util.Hashtable;

public class ColorDarknerFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Color Darkner";
	}

	@Override
	public Object create(Hashtable<String, Object> arguments) {
		return new ColorDarkner();
	}
}
