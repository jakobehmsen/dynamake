package dynamake.models;

import java.io.Serializable;

public interface Location<T> extends Serializable {
	T getChild(T holder);
	Location<T> forForwarding();
}
