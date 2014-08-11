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
	
	@Override
	public Location forForwarding() {
		return this;
	}
	
	@Override
	public String toString() {
		return ".";
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ModelRootLocation;
	}
	
	@Override
	public int hashCode() {
		return 117;
	}
}