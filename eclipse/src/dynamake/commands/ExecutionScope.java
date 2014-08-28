package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayDeque;

public class ExecutionScope implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ArrayDeque<Object> production = new ArrayDeque<Object>();
	
	public void produce(Object value) {
		production.addLast(value);
	}
	
	public Object consume() {
		return production.pollFirst();
	}

	public void unproduce() {
		production.removeLast();
	}

	public void unconsume(Object value) {
		production.addFirst(value);
	}
}
