package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Hashtable;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelRootLocation;

public class ExecutionScope<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static class NullWrapper {
		
	}

	private ArrayDeque<Object> production = new ArrayDeque<Object>();
	private Hashtable<String, Object> locals = new Hashtable<String, Object>();
	private Location<T> offset = new ModelRootLocation<T>();
	
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
	
	public void pushOffset(Location<T> offset) {
		this.offset = new CompositeLocation<T>(this.offset, offset);
	}
	
	public Location<T> popOffset() {
		CompositeLocation<T> offsetAsComposite = (CompositeLocation<T>)offset;
		Location<T> poppedOffset = offsetAsComposite.getTail();
		offset = offsetAsComposite.getHead();
		return poppedOffset; 
	}
	
	public Location<T> getOffset() {
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

	public ExecutionScope<Model> forForwarding() {
		// TODO Auto-generated method stub
		return null;
	}
}
