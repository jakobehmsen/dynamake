package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Model;

public interface MappableOutput extends Serializable {
	public Object mapToReferenceLocation(Model sourceReference, Model targetReference);
}
