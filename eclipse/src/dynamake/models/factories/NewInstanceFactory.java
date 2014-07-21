package dynamake.models.factories;

import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class NewInstanceFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location modelLocation;
	
	public NewInstanceFactory(Location modelLocation) {
		this.modelLocation = modelLocation;
	}

	@Override
	public String getName() {
		return "New Instance";
	}
	
	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		Model instance = inhereter.cloneDeep();
		
		inhereter.addObserver(new HistoryChangeForwarder(instance));
		
		return instance;
	}
}
