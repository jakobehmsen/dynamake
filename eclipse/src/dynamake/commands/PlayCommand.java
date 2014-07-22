package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PlayCommand implements Command<Model> {
	public static class AfterRewind implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			return new PlayCommand(((RewindCommand.Output)output).logPart);
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Model.PendingUndoablePair> pendingUndoablePairs;

	public PlayCommand(List<Model.PendingUndoablePair> pendingUndoablePairs) {
		this.pendingUndoablePairs = pendingUndoablePairs;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.play(pendingUndoablePairs, propCtx, 0, collector);
		
		return null;
	}
}
