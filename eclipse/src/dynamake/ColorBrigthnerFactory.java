package dynamake;

import java.util.Hashtable;

public class ColorBrigthnerFactory  implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Color Brightner";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new ColorBrightner();
	}
}
