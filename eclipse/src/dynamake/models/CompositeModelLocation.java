package dynamake.models;

import dynamake.TransactionFactory;
import dynamake.TransactionFactory.CompositeLocation;

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

	@Override
	public Location getModelComponentLocation() {
		return new CompositeLocation<Model>(head.getModelComponentLocation(), tail.getModelComponentLocation());
	}
}