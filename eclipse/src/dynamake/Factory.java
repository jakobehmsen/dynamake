package dynamake;

import java.io.Serializable;

public interface Factory extends Serializable {
	String getName();
	Object create();
}
