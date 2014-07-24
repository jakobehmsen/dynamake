package dynamake.commands;

import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class PlayCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
//	private List<Model.PendingUndoablePair> pendingUndoablePairs;
	private List<CommandState<Model>> commandStates;

//	public PlayCommand(List<Model.PendingUndoablePair> pendingUndoablePairs) {
//		this.pendingUndoablePairs = pendingUndoablePairs;
//	}
	
	public PlayCommand(List<CommandState<Model>> commandStates) {
		this.commandStates = commandStates;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.play(commandStates, propCtx, 0, collector);
		
		return null;
	}
}
