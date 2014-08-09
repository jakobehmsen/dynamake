package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Model;

public interface MappableForwardable extends Serializable {
	public MappableForwardable mapToReferenceLocation(Model sourceReference, Model targetReference);
	public MappableForwardable forForwarding();
}
