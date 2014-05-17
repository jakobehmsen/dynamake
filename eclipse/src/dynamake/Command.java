package dynamake;

import java.io.Serializable;
import java.util.Date;

public interface Command<T> extends Serializable {
	void executeOn(T prevalentSystem, Date executionTime);
	Command<T> antagonist();
}
