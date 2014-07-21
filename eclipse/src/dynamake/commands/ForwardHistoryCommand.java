package dynamake.commands;

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
		
		inhereter.addObserver(new HistoryChangeForwarder(inheretee));
		
		return null;
	}
}