package dynamake.commands;

import dynamake.models.Model;

public interface MappableCommandFactory<T> extends CommandFactory<T> {
	public CommandFactory<T> mapToReferenceLocation(Model sourceReference, Model targetReference);
}
