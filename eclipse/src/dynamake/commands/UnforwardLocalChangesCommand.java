package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnforwardLocalChangesCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location locationOfTarget;

	public UnforwardLocalChangesCommand(Location locationOfSource) {
		this.locationOfTarget = locationOfSource;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfTarget);
		Model target = (Model)location.getChild(prevalentSystem);

		LocalChangesForwarder historyChangeForwarder = new LocalChangesForwarder(source, target);
		source.removeObserverLike(historyChangeForwarder);
		target.removeObserverLike(historyChangeForwarder);
		
		return null;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfTarget);
		Location locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new UnforwardLocalChangesCommand(locationOfSourceFromTargetReference);
	}
}
