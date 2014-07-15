package dynamake.commands;

import java.util.List;

public interface DualCommand2Factory<T> {
	void createDualCommands(List<DualCommand2<T>> dualCommands);
}
