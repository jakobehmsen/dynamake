package dynamake.models.factories;

import dynamake.models.CanvasModel;
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
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		Model instance = inhereter.cloneDeep();
		
		inhereter.addObserver(new HistoryChangeForwarder(inhereter, instance));
		if(inhereter instanceof CanvasModel)
			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance);
		
		return instance;
	}
	
	private void forwardHistoryChangesToContainedModels(CanvasModel inhereterCanvas, CanvasModel inhereteeCanvas) {
		for(Location location: inhereterCanvas.getLocations()) {
			Model inhereterModel = inhereterCanvas.getModelByLocation(location);
			Model inhereteeModel = inhereteeCanvas.getModelByLocation(location);
			
			inhereterModel.addObserver(new HistoryChangeForwarder(inhereterModel, inhereteeModel));
			if(inhereterModel instanceof CanvasModel)
				forwardHistoryChangesToContainedModels((CanvasModel)inhereterModel, (CanvasModel)inhereterModel);
		}
	}
}
