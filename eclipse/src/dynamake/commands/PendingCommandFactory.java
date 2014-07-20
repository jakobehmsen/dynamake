package dynamake.commands;

import java.util.List;

public interface PendingCommandFactory<T> {
	T getReference();
	void createPendingCommand(List<CommandState<T>> commandStates);
}
