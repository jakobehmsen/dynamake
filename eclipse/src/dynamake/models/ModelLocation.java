package dynamake.models;

import java.io.Serializable;

public interface ModelLocation extends Location, Serializable {
	Location getModelComponentLocation();
}
