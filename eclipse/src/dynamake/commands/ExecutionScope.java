package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayDeque;

public class ExecutionScope implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static class NullWrapper {
		
	}

	private ArrayDeque<Object> production = new ArrayDeque<Object>();
	
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
}
