package dynamake.commands;

import java.util.List;

import dynamake.models.Location;

public interface CommandStateFactory<T> {
	T getReference();
	void createDualCommands(Location location, List<CommandState<T>> commandStates);
}