package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface CommandState<T> extends Serializable {
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location);
}
