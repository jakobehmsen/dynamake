package dynamake.commands;

public interface CommandStateWithOutput<T> extends CommandState<T> {
	Object getOutput();
}
