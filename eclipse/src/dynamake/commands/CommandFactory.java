package dynamake.commands;

import java.util.List;

public interface CommandFactory<T> {
	void createCommands(List<Command<T>> commands);
}
