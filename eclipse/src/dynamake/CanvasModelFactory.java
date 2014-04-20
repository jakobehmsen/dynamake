package dynamake;

import java.util.Hashtable;

public class CanvasModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Canvas";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new CanvasModel();
	}
}
