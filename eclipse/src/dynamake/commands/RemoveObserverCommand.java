package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class RemoveObserverCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location<Model> observableLocation;
		private Location<Model> observerLocation;
		
		public RemoveObserverCommand(Location<Model> observableLocation, Location<Model> observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
			Model observable = new CompositeLocation<Model>(location, observableLocation).getChild(rootPrevalentSystem);
			Model observer = new CompositeLocation<Model>(location, observerLocation).getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
//			System.out.println(observer + " no longer observes " + observable);
			
			// TODO: Consider whether a change should be sent out here
			return null;
		}
	}