package dynamake.commands;

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

	public UnforwardHistoryCommand(Location locationOfInhereter) {
		this.locationOfInhereter = locationOfInhereter;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)location.getChild(prevalentSystem);

		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, inheretee);
		inhereter.removeObserverLike(historyChangeForwarder);
		inheretee.removeObserverLike(historyChangeForwarder);
		
		return null;
	}

}
