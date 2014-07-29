package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;

public interface MappableOutput extends Serializable {
	public Object mapToReferenceLocation(Location referenceLocation);
}
