package dynamake.commands;

public interface Command2Factory<T> {
	Command2<T> createCommand(Object output);
}
