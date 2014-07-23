package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnplayCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Model.PendingUndoablePair> pendingUndoablePairs;

	public UnplayCommand(List<Model.PendingUndoablePair> pendingUndoablePairs) {
		this.pendingUndoablePairs = pendingUndoablePairs;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.unplay(pendingUndoablePairs, propCtx, 0, collector);
		
		return null;
	}
}
