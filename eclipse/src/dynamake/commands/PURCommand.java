package dynamake.commands;

public interface PURCommand<T> extends ReversibleCommand<T> {
	PURCommand<T> inReplayState();
	PURCommand<T> inUndoState();
	PURCommand<T> inRedoState();
	PURCommand<T> inNextState();
}
