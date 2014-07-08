package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberCollector;

// TODO: Consider renaming this type of command to ReversibleCommand and reflect this on the DualCommand* types.
public interface DualCommand<T> extends Serializable {
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberCollector<T> collector);
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberCollector<T> collector);
}
