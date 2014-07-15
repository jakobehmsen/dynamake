package dynamake.commands;

import java.util.List;

public interface CommandStateFactory<T> {
	void createDualCommands(List<CommandState<T>> dualCommands);
}
