package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Hashtable;

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
	private Hashtable<String, Object> locals = new Hashtable<String, Object>();
	private Location offset = new ModelRootLocation();
	
	public void produce(Object value) {
		if(value == null)
			value = new NullWrapper();
		
		production.push(value);
	}
	
	public Object consume() {
		// If pollLast returns null, then ambiguity of the return result of consume occurs, because
		// the cause of null is not NullWrapper.
		Object value =  production.pop();
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

	public Object store(String name) {
		Object value = consume();
		Object currentValue = locals.get(name);
		locals.put(name, value);
		return currentValue;
	}

	public void load(String name) {
		Object value = locals.get(name);
		produce(value);
	}

	public void restore(String name, Object value) {
		locals.put(name, value);
	}
}
