package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public interface Command<T> extends Serializable {
	Object executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location);
	
	public static class Null<T> implements Command<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, Collector<T> collector, Location location) {
			return null;
		}
	}
}
