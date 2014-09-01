package dynamake.commands;

import dynamake.models.LocalChangesForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnforwardLocalChangesCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		@SuppressWarnings("unchecked")
		Location<Model> locationOfSourceFromTarget = (Location<Model>)scope.consume();
		
		Model target = location.getChild(prevalentSystem);
		Model source = locationOfSourceFromTarget.getChild(target);

		LocalChangesForwarder historyChangeForwarder = new LocalChangesForwarder(source, target);
		source.removeObserverLike(historyChangeForwarder);
		target.removeObserverLike(historyChangeForwarder);
		
		System.out.println("Unforward local changes from " + source + " to " + target);
		
		scope.produce(locationOfSourceFromTarget);
		
		return null;
	}
}
