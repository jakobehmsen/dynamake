package dynamake.commands;

public interface UpwardableCommandFactory<T> extends CommandFactory<T> {
	CommandFactory<T> forUpwarding();
}
