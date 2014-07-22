package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RewindCommand implements Command<Model> {
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final List<Model.PendingUndoablePair> logPart;
		
		public Output(List<PendingUndoablePair> logPart) {
			this.logPart = logPart;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int steps;

	public RewindCommand(int steps) {
		this.steps = steps;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		List<Model.PendingUndoablePair> logPart = model.getLogBackwards(steps);
		model.rewind(steps, propCtx, 0, collector);
		
		return new Output(logPart);
	}
}
