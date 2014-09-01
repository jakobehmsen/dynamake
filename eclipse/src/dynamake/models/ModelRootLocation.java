package dynamake.models;


public class ModelRootLocation<T> implements Location<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public T getChild(T holder) {
		return holder;
	}
	
	@Override
	public Location<T> forForwarding() {
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