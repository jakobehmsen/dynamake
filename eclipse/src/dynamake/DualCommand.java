package dynamake;

import java.io.Serializable;
import java.util.Date;

public interface DualCommand<T> extends Serializable {
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime);
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime);
}