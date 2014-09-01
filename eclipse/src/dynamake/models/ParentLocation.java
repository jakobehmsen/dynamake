package dynamake.models;

public class ParentLocation implements Location<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Model getChild(Model holder) {
		return holder.getParent();
	}
	
	@Override
	public Location<Model> forForwarding() {
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
	
	@Override
	public String toString() {
		return "..";
	}
}
