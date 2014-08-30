package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class InjectCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Location targetLocation = (Location)scope.consume();
		Location sourceLocation = (Location)scope.consume();
		
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, sourceLocation);
		Model target = (Model)CompositeLocation.getChild(prevalentSystem, location, targetLocation);
		
		source.inject(target);
		
		// TODO: Consider whether a change should be sent out here
		return null;
	}
}
