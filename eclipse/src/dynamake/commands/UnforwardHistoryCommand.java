package dynamake.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnforwardHistoryCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location locationOfInhereter;
	private Location locationOfInheretee;

	public UnforwardHistoryCommand(Location locationOfInhereter, Location locationOfInheretee) {
		this.locationOfInhereter = locationOfInhereter;
		this.locationOfInheretee = locationOfInheretee;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInheretee);

		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, inheretee);
		inhereter.removeObserverLike(historyChangeForwarder);
		inheretee.removeObserverLike(historyChangeForwarder);
		historyChangeForwarder.attach(propCtx, 0, collector);
		
		return null;
	}

}
