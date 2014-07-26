package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PlayBackwardCommand2;
import dynamake.models.PlayForwardCommand2;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;
import dynamake.transcription.TranscribeOnlyAndPostNotPendingCommandFactory;

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
		final Model instance = inhereter.cloneBase();

		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, instance);
		inhereter.addObserver(historyChangeForwarder);
		instance.addObserver(historyChangeForwarder);
		historyChangeForwarder.attach(propCtx, propDistance, collector);
		if(inhereter instanceof CanvasModel)
			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
		
		ArrayList<Model.DualCommand> changesToInheret = new ArrayList<Model.DualCommand>();
		List<Model.DualCommand> inhereterInheretedChanges = (List<Model.DualCommand>)inhereter.getProperty("Inhereted");
		if(inhereterInheretedChanges != null)
			changesToInheret.addAll(inhereterInheretedChanges);
		List<Model.DualCommand> inhereterLocalChanges = inhereter.getLocalChanges();
		changesToInheret.addAll(inhereterLocalChanges);
		
		instance.playForwards2(changesToInheret, propCtx, propDistance, collector);
		instance.setProperty("Inhereted", changesToInheret, propCtx, propDistance, collector);
		
//		collector.execute(new TranscribeOnlyAndPostNotPendingCommandFactory<Model>() {
//			@Override
//			public Model getReference() {
//				return instance;
//			}
//
//			@Override
//			public void createPendingCommand(List<CommandState<Model>> commandStates) {
//				commandStates.add(new PendingCommandState<Model>(
//					new PlayForwardCommand2(changeToInheret), 
//					new PlayBackwardCommand2(changeToInheret)
//				));
//			}
//		});
		
		return instance;
	}
	
	private void forwardHistoryChangesToContainedModels(CanvasModel inhereterCanvas, CanvasModel inhereteeCanvas, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(Location location: inhereterCanvas.getLocations()) {
			Model inhereterModel = inhereterCanvas.getModelByLocation(location);
			Model inhereteeModel = inhereteeCanvas.getModelByLocation(location);

			HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereterModel, inhereteeModel);
			inhereterModel.addObserver(historyChangeForwarder);
			inhereteeModel.addObserver(historyChangeForwarder);
			historyChangeForwarder.attach(propCtx, propDistance, collector);
			if(inhereterModel instanceof CanvasModel)
				forwardHistoryChangesToContainedModels((CanvasModel)inhereterModel, (CanvasModel)inhereterModel, propCtx, propDistance, collector);
		}
	}
}
