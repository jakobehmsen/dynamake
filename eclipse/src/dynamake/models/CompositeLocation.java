package dynamake.models;

public class CompositeLocation<T> implements Location<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location<T> head;
	private Location<T> tail;
	
	public CompositeLocation(Location<T> head, Location<T> tail) {
		this.head = head;
		this.tail = tail;
	}
	
	public Location<T> getHead() {
		return head;
	}
	
	public Location<T> getTail() {
		return tail;
	}

	@Override
	public T getChild(T holder) {
		return tail.getChild(head.getChild(holder));
	}
	
	public static <T> T getChild(T holder, Location<T> head, Location<T> tail) {
		T headObj = head.getChild(holder);
		return tail.getChild(headObj);
	}
	
	@Override
	public Location<T> forForwarding() {
		return new CompositeLocation<T>(head.forForwarding(), tail.forForwarding());
	}
	
	@Override
	public String toString() {
		return head + "/" + tail;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof CompositeLocation) {
			@SuppressWarnings("unchecked")
			CompositeLocation<T> otherCompositeLocation = (CompositeLocation<T>)obj;
			return this.head.equals(otherCompositeLocation.head) && this.tail.equals(otherCompositeLocation.tail);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return head.hashCode() * tail.hashCode();
	}
}