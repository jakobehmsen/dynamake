package dynamake.commands;

import dynamake.models.Model;

public interface MappableCommand<T> extends Command<T> {
	public Command<T> mapToReferenceLocation(Model sourceReference, Model targetReference);
}
