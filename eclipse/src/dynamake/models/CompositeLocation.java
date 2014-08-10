package dynamake.models;

public class CompositeLocation implements Location {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location head;
	private Location tail;
	
	public CompositeLocation(Location head, Location tail) {
		this.head = head;
		this.tail = tail;
	}

	@Override
	public Object getChild(Object holder) {
		return tail.getChild(head.getChild(holder));
	}
	
	public static Object getChild(Object holder, Location head, Location tail) {
		Object headObj = head.getChild(holder);
		return tail.getChild(headObj);
	}
	
	@Override
	public Location forForwarding() {
		return new CompositeLocation(head.forForwarding(), tail.forForwarding());
	}
	
	@Override
	public String toString() {
		return head + "/" + tail;
	}
}