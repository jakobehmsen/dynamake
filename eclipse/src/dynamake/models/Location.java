package dynamake.models;

import java.io.Serializable;

public interface Location extends Serializable {
	Object getChild(Object holder);
}
