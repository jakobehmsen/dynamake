package dynamake.models;


public class ModelRootLocation implements Location {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object getChild(Object holder) {
		return holder;
	}
}