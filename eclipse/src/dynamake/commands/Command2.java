package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface Command2<T> {
	void executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, Command2Scope scope);
}
