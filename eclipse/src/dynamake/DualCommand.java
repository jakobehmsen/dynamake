package dynamake;

import java.io.Serializable;
import java.util.Date;

public interface DualCommand<T> extends Serializable {
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch);
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch);
	public boolean occurredWithin(Location location);
}
