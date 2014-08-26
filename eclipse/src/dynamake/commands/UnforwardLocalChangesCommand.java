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

	private Location locationOfSourceFromTarget;

	public UnforwardLocalChangesCommand(Location locationOfSourceFromTarget) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Model target = (Model)location.getChild(prevalentSystem);
		Model source = (Model)locationOfSourceFromTarget.getChild(target);

		LocalChangesForwarder historyChangeForwarder = new LocalChangesForwarder(source, target);
		source.removeObserverLike(historyChangeForwarder);
		target.removeObserverLike(historyChangeForwarder);
		
		System.out.println("Unforward local changes from " + source + " to " + target);
		
		return null;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfSourceFromTarget);
		Location locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new UnforwardLocalChangesCommand(locationOfSourceFromTargetReference);
	}
}
