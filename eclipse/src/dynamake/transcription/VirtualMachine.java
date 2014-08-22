package dynamake.transcription;

import java.io.Serializable;
import java.util.Stack;

import dynamake.models.Location;
import dynamake.models.Model;

public class VirtualMachine<T> {
	public interface VMInstruction extends Serializable {
		void executeForward(VMProcess scope);
		void executeBackward(VMProcess scope);
	}
	
	public static class Instruction {
		public static final int TYPE_PUSH_REF_LOC = 0;
		public static final int TYPE_PUSH = 1;
		public static final int TYPE_POP = 2;
		public static final int TYPE_DUP = 3;
		public static final int TYPE_SWAP = 4;
		public static final int TYPE_STOP = 5;
		public static final int TYPE_JUMP = 6;
		public static final int TYPE_RET = 7;
		public static final int TYPE_EXIT = 8;
		public static final int TYPE_BEGIN_LOG = 9;
		public static final int TYPE_POST = 10;
		public static final int TYPE_END_LOG = 11;
		public static final int TYPE_COMMIT = 12;
		public static final int TYPE_REJECT = 13;
		public static final int TYPE_CUSTOM = 14;
		
		public final int type;
		public final Object operand;

		public Instruction(int type) {
			this.type = type;
			this.operand = null;
		}

		public Instruction(int type, Object operand) {
			this.type = type;
			this.operand = operand;
		}
	}
	
	private interface VMProcess {
		void pushReferenceLocation();
		void push(Object value);
		Object pop();
		Object peek();
		void dup();
		void swap();
		void stop();
		void jump(int delta);
		void ret();
		void execute(Instruction[] body);
	}
	
	private static class Scope {
		public final Location referenceLocation;
		public final Stack<Object> stack = new Stack<Object>();
		public int i;
		public final Instruction[] body;
		
		public Scope(Location referenceLocation, Instruction[] body) {
			this.referenceLocation = referenceLocation;
			this.body = body;
		}
	}
	
	private static class ScopedProcess implements VMProcess {
		public Scope currentScope;
		public Stack<Scope> scopeStack = new Stack<Scope>();

		@Override
		public void pushReferenceLocation() {
			push(currentScope.referenceLocation);
		}

		@Override
		public void push(Object value) {
			currentScope.stack.push(value);
		}

		@Override
		public Object pop() {
			return currentScope.stack.pop();
		}

		@Override
		public Object peek() {
			return currentScope.stack.peek();
		}

		@Override
		public void dup() {
			currentScope.stack.push(currentScope.stack.peek());
		}

		@Override
		public void swap() {
			Object top = currentScope.stack.get(currentScope.stack.size() - 1);
			currentScope.stack.set(currentScope.stack.size() - 1, currentScope.stack.get(currentScope.stack.size() - 2));
			currentScope.stack.set(currentScope.stack.size() - 2, top);
		}

		@Override
		public void stop() {
			Scope stoppedScope = currentScope;
			currentScope = scopeStack.pop();
			currentScope.stack.push(stoppedScope); // Push scope as "continuation"
		}

		@Override
		public void jump(int delta) {
			currentScope.i += delta;
		}
		
		@Override
		public void ret() {
			currentScope = scopeStack.pop();
		}

		@Override
		public void execute(Instruction[] body) {
			scopeStack.push(new Scope(currentScope.referenceLocation, body));
		}
	}
	
	public void execute(T reference, Instruction[] body) {
		ScopedProcess process = new ScopedProcess();
		
		Location referenceLocation = ((Model)reference).getLocator().locate();
		process.scopeStack.push(new Scope(referenceLocation, body));
		boolean exitRequested = false;
		
		testShouldExit:
		if(!exitRequested) {
			while(true) {
				Instruction instruction = process.currentScope.body[process.currentScope.i];
				
				switch(instruction.type) {
				case Instruction.TYPE_PUSH_REF_LOC:
					process.currentScope.stack.push(process.currentScope.referenceLocation);
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_PUSH:
					process.currentScope.stack.push(instruction.operand);
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_POP:
					process.currentScope.stack.pop();
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_DUP:
					process.currentScope.stack.push(process.currentScope.stack.peek());
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_SWAP:
					Object top = process.currentScope.stack.get(process.currentScope.stack.size() - 1);
					process.currentScope.stack.set(process.currentScope.stack.size() - 1, process.currentScope.stack.get(process.currentScope.stack.size() - 2));
					process.currentScope.stack.set(process.currentScope.stack.size() - 2, top);
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_STOP:
					Scope stoppedScope = process.currentScope;
					process.currentScope = process.scopeStack.pop();
					process.currentScope.stack.push(stoppedScope); // Push scope as "continuation"
					// Otherwise, probably, history handler needs to be invoked here (instead)
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_JUMP:
					process.currentScope.i += (int)instruction.operand;

					continue;
				case Instruction.TYPE_RET:
					process.currentScope = process.scopeStack.pop();
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_EXIT:
					exitRequested = true;
					break testShouldExit;
					

				case Instruction.TYPE_BEGIN_LOG:
					// Somehow, start a new log of some kind (for instance for adding new history, or changing the existing history, of a model)
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_POST:
					// Somehow, pop and post to the log 
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_END_LOG:
					// Somehow, tell the logger to commit
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_COMMIT:
					// Persist changes made since last commit
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_REJECT:
					// Rollback changes made since last commit
					
					process.currentScope.i++;
					continue;
					
				case Instruction.TYPE_CUSTOM:
					VMInstruction customInstruction = (VMInstruction)instruction.operand;
					customInstruction.executeForward(process);

					process.currentScope.i++;
					continue;
				}
			}
		}
	}
}
