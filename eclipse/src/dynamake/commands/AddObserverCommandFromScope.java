package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AddObserverCommandFromScope implements Command<Model> {
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
		
		observable.addObserver(observer);
//		System.out.println(observer + " now observes " + observable);
		
		return null;
	}
}
