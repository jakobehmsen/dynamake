package dynamake;

import java.io.Serializable;
import java.util.Hashtable;

public interface Factory extends Serializable {
	// Should provide parametric information?
	// - In general, constraints?
	
	// With such parameters (and constraints, in general), it would be possible to implicitly support creation of an intermediate CreationModel
	
	String getName();
	Object create(Hashtable<String, Object> arguments);
}
