package dynamake.commands;

import dynamake.models.Location;

public interface MappableCommandFactory<T> extends CommandFactory<T> {
	public CommandFactory<T> mapToReferenceLocation(Location referenceLocation);
}
