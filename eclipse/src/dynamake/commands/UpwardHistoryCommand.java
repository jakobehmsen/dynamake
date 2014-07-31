package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesForwarder;
import dynamake.models.HistoryChangeUpwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UpwardHistoryCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location locationOfRootInhereter;
	private Location locationOfInhereter;
	private Location locationOfInheretee;

	public UpwardHistoryCommand(Location locationOfRootInhereter, Location locationOfInhereter, Location locationOfInheretee) {
		this.locationOfRootInhereter = locationOfRootInhereter;
		this.locationOfInhereter = locationOfInhereter;
		this.locationOfInheretee = locationOfInheretee;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model rootInhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfRootInhereter);
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
//		Model inheretee = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInheretee);
		
		// Use locationOfInheretee somehow for the HistoryChangeUpwarder
		locationOfInheretee.toString();
		LocalChangesForwarder historyChangeForwarder = rootInhereter.<LocalChangesForwarder>getObserverOfType(LocalChangesForwarder.class);
		
		inhereter.addObserver(new HistoryChangeUpwarder(historyChangeForwarder));
		
		return null;
	}
}
