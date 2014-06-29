package dynamake;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;

public interface DualCommand<T> extends Serializable {
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberBranch<T> branch);
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberBranch<T> branch);
	public boolean occurredWithin(Location location);
}
