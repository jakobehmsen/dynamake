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
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		@SuppressWarnings("unchecked")
		Location<Model> targetLocation = (Location<Model>)scope.consume();
		@SuppressWarnings("unchecked")
		Location<Model> sourceLocation = (Location<Model>)scope.consume();
		
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, sourceLocation);
		Model target = (Model)CompositeLocation.getChild(prevalentSystem, location, targetLocation);
		
		source.inject(target);
		
		// TODO: Consider whether a change should be sent out here
		return null;
	}
}
