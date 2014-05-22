package dynamake;

import java.util.List;

public interface DualCommandFactory<T> {
//	DualCommand<T> createDualCommand();
	void createDualCommands(List<DualCommand<T>> dualCommands);
}
