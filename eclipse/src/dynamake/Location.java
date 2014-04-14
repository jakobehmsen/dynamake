package dynamake;

import java.io.Serializable;

public interface Location extends Serializable {
	Object getChild(Object holder);
	void setChild(Object holder, Object child);
}
