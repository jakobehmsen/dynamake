package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RemoveObserverCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Location observerLocation = (Location)scope.consume();
		Location observableLocation = (Location)scope.consume();

		Model observable = (Model)new CompositeLocation(location, observableLocation).getChild(prevalentSystem);
		Model observer = (Model)new CompositeLocation(location, observerLocation).getChild(prevalentSystem);
		
		observable.removeObserver(observer);
//		System.out.println(observer + " no longer observes " + observable);
		
		return null;
	}
}
