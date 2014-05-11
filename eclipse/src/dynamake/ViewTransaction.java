package dynamake;

import java.io.Serializable;
import java.util.Date;

public interface ViewTransaction extends Serializable {
	void executeOn(Model prevalentSystem, Date executionTime, ViewManager viewManager);
}
