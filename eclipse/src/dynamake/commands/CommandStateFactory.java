package dynamake.commands;

import java.util.List;

public interface CommandStateFactory<T> {
	T getReference();
	void createDualCommands(List<CommandState<T>> dualCommands);
}
