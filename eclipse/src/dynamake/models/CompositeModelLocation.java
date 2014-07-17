package dynamake.models;

import dynamake.models.ModelTranscriber.CompositeLocation;

public class CompositeModelLocation implements ModelLocation {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ModelLocation head;
	private ModelLocation tail;
	
	public CompositeModelLocation(ModelLocation head, ModelLocation tail) {
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
}