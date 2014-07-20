package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

/**
 * Instances of implementors are supposed to represent commands in different states. 
 * For now, these states include pending, undoable, and redoable.
 * 
 * Further, when such instances are executed, a new command state is returned representing the state of a command in state relation context.
 * For now, such relation context is as follows: pending => (undoable => redoable)*
 
 * @param <T> The type of object that instances of implementers should support execution on.
 */
public interface CommandState<T> extends Serializable {
	public CommandState<T> executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location);
}
