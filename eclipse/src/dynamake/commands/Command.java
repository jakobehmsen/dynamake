package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

/**
 * Instances of implementors each is able to execute on objects and are supposed to be kept elsewhere for e.g. undo/do - thus the Serializable attribute.
 *
 * @param <T> The type of object that instances of implementers should support execution on.
 */
public interface Command<T> extends Serializable {
	Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope);
	
	public static class Null<T> implements Command<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, T prevalentSystem, Collector<T> collector, Location location, ExecutionScope scope) {
			return null;
		}
	}
}
