package dynamake.commands;

public interface ForwardableCommandFactory<T> extends CommandFactory<T> {
	CommandFactory<T> forForwarding(Object output);
}
