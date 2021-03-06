package dynamake.transcription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

import dynamake.models.Location;
import dynamake.models.Model;

public class VirtualMachine<T> {
	public interface VMInstruction<T> extends Serializable {
		void executeForward(VMProcess<T> scope);
		void executeBackward(VMProcess<T> scope);
	}
	
	public static class Instruction {
		public static final int TYPE_PUSH_REF_LOC = 0;
		public static final int TYPE_PUSH = 1;
		public static final int TYPE_POP = 2;
		public static final int TYPE_DUP = 3;
		public static final int TYPE_SWAP = 4;
		public static final int TYPE_STOP = 5;
		public static final int TYPE_JUMP = 6;
		public static final int TYPE_RETURN = 7;
		public static final int TYPE_RETURN_TO = 8;
		public static final int TYPE_FINISH = 9;
		public static final int TYPE_COMMIT = 10;
		public static final int TYPE_REJECT = 11;
		public static final int TYPE_CONTINUE = 12;
		public static final int TYPE_CUSTOM = 13;
		
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
	
	private static class ForwardBackwardPair {
		@SuppressWarnings("unused")
		public final Instruction forward; 
		@SuppressWarnings("unused")
		public final Instruction backward;
		
		public ForwardBackwardPair(Instruction forward, Instruction backward) {
			this.forward = forward;
			this.backward = backward;
		}
	}
	
	private interface VMProcess<T> {
		void pushReferenceLocation();
		void push(Object value);
		Object pop();
		Object peek();
		void dup();
		void swap();
		void execute(T reference, Instruction[] body);
	}
	
	private static class Scope<T> {
		public final Location<T> referenceLocation;
		public final Stack<Object> stack = new Stack<Object>();
		public int i;
		public final Instruction[] body;
		
		public Scope(Location<T> referenceLocation, Instruction[] body) {
			this.referenceLocation = referenceLocation;
			this.body = body;
		}
	}
	
	private static class ScopedProcess<T> implements VMProcess<T> {
		public Scope<T> currentScope;
		public Stack<Scope<T>> scopeStack = new Stack<Scope<T>>();

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
			// Somehow, the popped must be repushable in rollback scenarios
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
		public void execute(T reference, Instruction[] body) {
			@SuppressWarnings("unchecked")
			Location<T> referenceLocation = (Location<T>)((Model)reference).getLocator().locate();
			scopeStack.push(new Scope<T>(referenceLocation, body));
		}
	}
	
	private ArrayList<ForwardBackwardPair> executionLog = new ArrayList<ForwardBackwardPair>();
	
	public void execute(T reference, Instruction[] body) {
		ScopedProcess<T> process = new ScopedProcess<T>();
		
		process.execute(reference, body);
		
		boolean exitRequested = false;
		testShouldExit:
		if(!exitRequested) {
			while(true) {
				Instruction instruction = process.currentScope.body[process.currentScope.i];
				
				switch(instruction.type) {
				case Instruction.TYPE_PUSH_REF_LOC:
					process.currentScope.stack.push(process.currentScope.referenceLocation);
					
					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_POP)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_PUSH:
					process.currentScope.stack.push(instruction.operand);

					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_POP)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_POP:
					Object value = process.currentScope.stack.pop();

					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_PUSH, value)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_DUP:
					process.currentScope.stack.push(process.currentScope.stack.peek());

					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_POP)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_SWAP:
					Object top = process.currentScope.stack.get(process.currentScope.stack.size() - 1);
					process.currentScope.stack.set(process.currentScope.stack.size() - 1, process.currentScope.stack.get(process.currentScope.stack.size() - 2));
					process.currentScope.stack.set(process.currentScope.stack.size() - 2, top);

					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_SWAP)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_STOP:
					Scope<T> stoppedScope = process.currentScope;
					stoppedScope.i++; // Move to next instruction
					process.currentScope = process.scopeStack.pop();

					// Should probably be a reversible version of stoppedScope?...
					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_CONTINUE, stoppedScope)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_JUMP:
					process.currentScope.i += (int)instruction.operand;
					
					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_JUMP, -(int)instruction.operand)));
					continue;
				case Instruction.TYPE_RETURN:
					Scope<T> scopeReturnedFrom = process.currentScope;
					process.currentScope = process.scopeStack.pop();
					
					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_RETURN_TO, scopeReturnedFrom)));
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_RETURN_TO: {
					@SuppressWarnings("unchecked")
					Scope<T> scope = (Scope<T>)instruction.operand;
					process.scopeStack.push(scope);
					
					process.currentScope.i++;
					continue;
				} case Instruction.TYPE_FINISH:
					exitRequested = true;
					break testShouldExit;
				case Instruction.TYPE_COMMIT:
					// Persist changes made since last commit
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_REJECT:
					// Rollback changes made since last commit
					
					// Push execution of backward commands in reverse order - but don't track the execution
					
					process.currentScope.i++;
					continue;
				case Instruction.TYPE_CONTINUE: {
					@SuppressWarnings("unchecked")
					Scope<T> scope = (Scope<T>)process.currentScope.stack.pop();
					process.scopeStack.push(scope);
					executionLog.add(new ForwardBackwardPair(instruction, new Instruction(Instruction.TYPE_STOP)));

					process.currentScope.i++;
					continue;
				} case Instruction.TYPE_CUSTOM:
					@SuppressWarnings("unchecked")
					VMInstruction<T> customInstruction = (VMInstruction<T>)instruction.operand;
					customInstruction.executeForward(process);
					executionLog.add(new ForwardBackwardPair(instruction, instruction));

					process.currentScope.i++;
					continue;
				}
			}
		}
	}
}
