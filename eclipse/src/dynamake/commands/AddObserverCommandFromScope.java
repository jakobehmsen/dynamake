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
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		@SuppressWarnings("unchecked")
		Location<Model> observerLocation = (Location<Model>)scope.consume();
		@SuppressWarnings("unchecked")
		Location<Model> observableLocation = (Location<Model>)scope.consume();
		
		Model observable = new CompositeLocation<Model>(location, observableLocation).getChild(prevalentSystem);
		Model observer = new CompositeLocation<Model>(location, observerLocation).getChild(prevalentSystem);
		
		observable.addObserver(observer);
//		System.out.println(observer + " now observes " + observable);
		
		scope.produce(observableLocation);
		scope.produce(observerLocation);
		
		return null;
	}
}
