package dynamake.commands;

import java.util.Stack;

import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ForwardHistoryCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location locationOfInhereter;
	private Location locationOfInheretee;

	public ForwardHistoryCommand(Location locationOfInhereter, Location locationOfInheretee) {
		this.locationOfInhereter = locationOfInhereter;
		this.locationOfInheretee = locationOfInheretee;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInheretee);
		
		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, inheretee);
		inhereter.addObserver(historyChangeForwarder);
		inheretee.addObserver(historyChangeForwarder);
		inheretee.setProperty("inhereterUndoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		inheretee.setProperty("inhereterRedoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		inheretee.setProperty("observeInheretee", true, propCtx, 0, collector);
		inheretee.setProperty("doingUndoRedo", false, propCtx, 0, collector);
		
		return null;
	}
}