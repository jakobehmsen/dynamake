package dynamake.commands;

public interface UpwardableCommand<T> extends Command<T> {
	Command<T> forUpwarding();
}
