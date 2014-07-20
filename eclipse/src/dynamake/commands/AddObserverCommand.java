package dynamake.commands;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AddObserverCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public AddObserverCommand(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location location) {
			Model observable = (Model)new CompositeLocation(location, observableLocation).getChild(rootPrevalentSystem);
			Model observer = (Model)new CompositeLocation(location, observerLocation).getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
//			System.out.println(observer + " now observes " + observable);
			
			// TODO: Consider whether a change should be sent out here
			return null;
		}
	}