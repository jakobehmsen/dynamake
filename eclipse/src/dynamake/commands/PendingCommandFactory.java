package dynamake.commands;

import java.util.List;

/**
 * Instances of implementors are supposed to be able to commands in a pending state.
 */
public interface PendingCommandFactory<T> {
	T getReference();
	void createPendingCommands(List<CommandState<T>> commandStates);
}
