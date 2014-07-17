package dynamake.models;


public class ModelRootLocation implements ModelLocation {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object getChild(Object holder) {
		return holder;
	}
}