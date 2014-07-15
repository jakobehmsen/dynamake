package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface Command2<T> extends Serializable {
	Object executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location);
}
