package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ForwardHistoryCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location locationOfInhereter;

	public ForwardHistoryCommand(Location locationOfInhereter) {
		this.locationOfInhereter = locationOfInhereter;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)location.getChild(prevalentSystem);
		
		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, inheretee);
		inhereter.addObserver(historyChangeForwarder);
		inheretee.addObserver(historyChangeForwarder);
		historyChangeForwarder.attach(propCtx, 0, collector);
		
//		// Should be done recursively upwards in the inheritance chain
//		List<CommandState<Model>> changesToInheret = inhereter.getLocalChanges();
//		inheretee.playThenReverse(changesToInheret, propCtx, 0, collector);
		
		System.out.println("Forward history from " + inhereter + " to " + inheretee);
		
		return null;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model inhereter = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfInhereter);
		Location locationOfInhereterFromTargetReference = ModelComponent.Util.locationBetween(targetReference, inhereter);
		
		return new ForwardHistoryCommand(locationOfInhereterFromTargetReference);
	}
}