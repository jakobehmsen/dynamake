package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayDeque;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.ModelRootLocation;

public class ExecutionScope implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static class NullWrapper {
		
	}

	private ArrayDeque<Object> production = new ArrayDeque<Object>();
	private Location offset = new ModelRootLocation();
	
	public void produce(Object value) {
		if(value == null)
			value = new NullWrapper();
		
		production.addLast(value);
	}
	
	public Object consume() {
		// If pollLast returns null, then ambiguity of the return result of consume occurs, because
		// the cause of null is not NullWrapper.
		Object value =  production.pollLast();
		return value instanceof NullWrapper ? null : value;
	}
	
	public void pushOffset(Location offset) {
		this.offset = new CompositeLocation(this.offset, offset);
	}
	
	public Location popOffset() {
		CompositeLocation offsetAsComposite = (CompositeLocation)offset;
		Location poppedOffset = offsetAsComposite.getTail();
		offset = offsetAsComposite.getHead();
		return poppedOffset; 
	}
	
	public Location getOffset() {
		return offset;
	}
}
