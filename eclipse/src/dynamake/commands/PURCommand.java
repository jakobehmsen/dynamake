package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.models.transcription.PostOnlyTransactionHandler;
import dynamake.transcription.Collector;

// PUR is shorthand for Pending, Undo, Redo
public class PURCommand<T> implements Command<T> {
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

	public PURCommand(ReversibleCommand<T> pending, ReversibleCommand<T> undo, ReversibleCommand<T> redo) {
		this(STATE_PENDING, pending, undo, redo);
	}

	private PURCommand(int state, ReversibleCommand<T> pending, ReversibleCommand<T> undo, ReversibleCommand<T> redo) {
		this.state = state;
		this.pending = pending;
		this.undo = undo;
		this.redo = redo;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
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
		
		// TODO Auto-generated method stub
		return null;
	}

	public PURCommand<T> inUndoState() {
		return new PURCommand<T>(STATE_UNDO, pending, undo, redo);
	}

	public PURCommand<T> inRedoState() {
		return new PURCommand<T>(STATE_REDO, pending, undo, redo);
	}
}
