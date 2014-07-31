package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Model;

public interface Mappable extends Serializable {
	public Mappable mapToReferenceLocation(Model sourceReference, Model targetReference);
}
