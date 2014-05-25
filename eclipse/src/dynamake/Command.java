package dynamake;

import java.io.Serializable;
import java.util.Date;

public interface Command<T> extends Serializable {
	void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceConnection<T> connection);
//	Command<T> antagonist();
}
