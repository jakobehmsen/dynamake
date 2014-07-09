package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface DualCommand<T> extends Serializable {
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector);
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector);
}
