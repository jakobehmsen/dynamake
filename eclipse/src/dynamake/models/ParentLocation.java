package dynamake.models;

public class ParentLocation implements Location {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object getChild(Object holder) {
		return ((Model)holder).getParent();
	}
}
