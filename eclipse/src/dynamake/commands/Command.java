package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberCollector;

public interface Command<T> extends Serializable {
	void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberCollector<T> collector);
	
	public static class Null<T> implements Command<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberCollector<T> collector) {

		}
	}
}
