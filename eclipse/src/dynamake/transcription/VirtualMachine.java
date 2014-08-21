package dynamake.transcription;

import java.io.Serializable;
import java.util.Stack;

import dynamake.models.Location;

public class VirtualMachine<T> {
	public interface VMCommand extends Serializable {
		void execute(VMScope scope);
	}
	
	private static class Scope {
		public final Location reference;
		public final Stack<Object> stack = new Stack<Object>();
		public int i;
		public final VMCommand[] body;
		public boolean stopRequested;
		
		public Scope(Location reference, VMCommand[] body) {
			this.reference = reference;
			this.body = body;
		}
	}
	
	private interface VMScope {
		void pushReferenceLocation();
		void push(Object value);
		Object pop();
		Object peek();
		void dup();
		void swap();
		void stop();
		void jump(int delta);
		
		void execute(VMCommand command);
	}
	
	public void execute(T reference, VMCommand command) {
		
	}
}
