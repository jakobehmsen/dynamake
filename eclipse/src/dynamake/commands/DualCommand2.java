package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface DualCommand2<T> extends Serializable {
	public DualCommand2<T> executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector);
}
