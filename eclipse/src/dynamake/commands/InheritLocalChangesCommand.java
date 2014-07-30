package dynamake.commands;

import java.util.List;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class InheritLocalChangesCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location locationOfInhereter;

	public InheritLocalChangesCommand(Location locationOfInhereter) {
		this.locationOfInhereter = locationOfInhereter;
	}
	
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfInhereter);
		Model inheretee = (Model)location.getChild(prevalentSystem);
		
		List<CommandState<Model>> reversedInheritedChanges = inheretee.playThenReverse(inhereter.getLocalChanges(), propCtx, 0, collector);
		
		System.out.println(inheretee + " inherited from " + inhereter);
		
		return new PlayThenReverseCommand(reversedInheritedChanges);
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model inhereter = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation(), locationOfInhereter);
		Location locationOfInhereterFromTargetReference = ModelComponent.Util.locationBetween(targetReference, inhereter);
		
		return new InheritLocalChangesCommand(locationOfInhereterFromTargetReference);
	}
}
