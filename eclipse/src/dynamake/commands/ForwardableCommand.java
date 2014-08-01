package dynamake.commands;

public interface ForwardableCommand<T> extends Command<T> {
	Command<T> forForwarding(Object output);
}
