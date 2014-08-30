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
		Object value =  production.pollFirst();
		return value instanceof NullWrapper ? null : value;
	}

	public void unproduce() {
		production.removeLast();
	}

	public void unconsume(Object value) {
		production.addFirst(value);
	}
}
