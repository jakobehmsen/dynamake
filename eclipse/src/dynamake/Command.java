package dynamake;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;

public interface Command<T> extends Serializable {
	void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberBranch<T> branch);
	boolean occurredWithin(Location location);
	
	public static class Null<T> implements Command<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void executeOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, TranscriberBranch<T> branch) {

		}

		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
}
