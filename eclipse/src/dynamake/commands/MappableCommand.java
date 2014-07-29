package dynamake.commands;

import dynamake.models.Location;

public interface MappableCommand<T> extends Command<T> {
	public Command<T> mapToReferenceLocation(Location referenceLocation);
}
