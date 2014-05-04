package dynamake;

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
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new FloatingTextModel();
	}
}
