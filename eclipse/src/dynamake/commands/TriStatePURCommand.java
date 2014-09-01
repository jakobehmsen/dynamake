package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.transcription.PostOnlyTransactionHandler;
import dynamake.transcription.Collector;

// PUR is shorthand for Pending, Undo, Redo
// Should each tri state pur command have its own scope command associated to it?
// - and then supply this scope to respective executions of its pending-, undo-, and redo command? 
public class TriStatePURCommand<T> implements PURCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int STATE_PENDING = 0;
	public static final int STATE_UNDO = 1;
	public static final int STATE_REDO = 2;
	
	private int state;
	private ReversibleCommand<T> pending;
	private ReversibleCommand<T> undo;
	private ReversibleCommand<T> redo;

	public TriStatePURCommand(ReversibleCommand<T> pending, ReversibleCommand<T> undo, ReversibleCommand<T> redo) {
		this(STATE_PENDING, pending, undo, redo);
	}

	private TriStatePURCommand(int state, ReversibleCommand<T> pending, ReversibleCommand<T> undo, ReversibleCommand<T> redo) {
		this.state = state;
		this.pending = pending;
		this.undo = undo;
		this.redo = redo;
	}

	@Override
	public void executeForward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
		@SuppressWarnings("unchecked")
		T reference = (T)location.getChild(prevalentSystem);
		
		collector.startTransaction(reference, PostOnlyTransactionHandler.class);
		
		switch(state) {
		case STATE_PENDING:
			collector.execute(pending);
			break;
		case STATE_UNDO:
			collector.execute(undo);
			break;
		case STATE_REDO:
			collector.execute(redo);
			break;
		}
		
		collector.commitTransaction();
	}

	@Override
	public void executeBackward(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
		@SuppressWarnings("unchecked")
		T reference = (T)location.getChild(prevalentSystem);
		
		collector.startTransaction(reference, PostOnlyTransactionHandler.class);
		
		switch(state) {
		case STATE_PENDING:
			collector.execute(undo);
			break;
		case STATE_UNDO:
			collector.execute(redo);
			break;
		case STATE_REDO:
			collector.execute(undo);
			break;
		}
		
		collector.commitTransaction();
	}

	@Override
	public PURCommand<T> inReplayState() {
		return new TriStatePURCommand<T>(STATE_PENDING, pending, undo, redo);
	}

	@Override
	public PURCommand<T> inUndoState() {
		return new TriStatePURCommand<T>(STATE_UNDO, pending, undo, redo);
	}

	@Override
	public PURCommand<T> inRedoState() {
		return new TriStatePURCommand<T>(STATE_REDO, pending, undo, redo);
	}

	@Override
	public BaseValue<T> forForwarding() {
		return new TriStatePURCommand<T>(STATE_PENDING, 
			(ReversibleCommand<T>)pending.forForwarding(), 
			(ReversibleCommand<T>)undo.forForwarding(), 
			(ReversibleCommand<T>)redo.forForwarding());
	}

	@Override
	public BaseValue<T> mapToReferenceLocation(T source, T target) {
		return new TriStatePURCommand<T>(STATE_PENDING, 
			(ReversibleCommand<T>)pending.mapToReferenceLocation(source, target), 
			(ReversibleCommand<T>)undo.mapToReferenceLocation(source, target), 
			(ReversibleCommand<T>)redo.mapToReferenceLocation(source, target));
	}
}
