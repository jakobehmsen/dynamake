package dynamake.models.factories;

import java.util.ArrayList;
import java.util.Stack;

import dynamake.commands.CommandState;
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

		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, instance);
		inhereter.addObserver(historyChangeForwarder);
		instance.addObserver(historyChangeForwarder);
		instance.setProperty("inhereterUndoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		instance.setProperty("inhereterRedoStack", new Stack<CommandState<Model>>(), propCtx, 0, collector);
		instance.setProperty("observeInheretee", true, propCtx, 0, collector);
		instance.setProperty("doingUndoRedo", false, propCtx, 0, collector);
		instance.setProperty("localChanges", new ArrayList<CommandState<Model>>(), propCtx, 0, collector);
		if(inhereter instanceof CanvasModel)
			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
		
		return instance;
	}
	
	private void forwardHistoryChangesToContainedModels(CanvasModel inhereterCanvas, CanvasModel inhereteeCanvas, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(Location location: inhereterCanvas.getLocations()) {
			Model inhereterModel = inhereterCanvas.getModelByLocation(location);
			Model inhereteeModel = inhereteeCanvas.getModelByLocation(location);

			HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereterModel, inhereteeModel);
			inhereterModel.addObserver(historyChangeForwarder);
			inhereteeModel.addObserver(historyChangeForwarder);
			inhereteeModel.setProperty("inhereterUndoStack", new Stack<CommandState<Model>>(), propCtx, propDistance, collector);
			inhereteeModel.setProperty("inhereterRedoStack", new Stack<CommandState<Model>>(), propCtx, propDistance, collector);
			inhereteeModel.setProperty("observeInheretee", true, propCtx, 0, collector);
			inhereteeModel.setProperty("doingUndoRedo", false, propCtx, 0, collector);
			inhereteeModel.setProperty("localChanges", new ArrayList<CommandState<Model>>(), propCtx, 0, collector);
			if(inhereterModel instanceof CanvasModel)
				forwardHistoryChangesToContainedModels((CanvasModel)inhereterModel, (CanvasModel)inhereterModel, propCtx, propDistance, collector);
		}
	}
}
