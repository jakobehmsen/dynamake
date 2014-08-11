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
	
	@Override
	public Location forForwarding() {
		return this;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParentLocation;
	}
	
	@Override
	public int hashCode() {
		return 1117;
	}
}
