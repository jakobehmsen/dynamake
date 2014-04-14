package dynamake;

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
	public Object create() {
		return new CanvasModel();
	}
}
