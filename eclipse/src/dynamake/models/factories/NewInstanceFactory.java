package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.HistoryChangeForwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class NewInstanceFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public NewInstanceFactory(RectangleF creationBounds, Location modelLocation) {
		this.creationBounds = creationBounds;
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
		
		ArrayList<CommandState<Model>> changesToInheret = new ArrayList<CommandState<Model>>();
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> inhereterInheretedChanges = (List<CommandState<Model>>)inhereter.getProperty("Inhereted");
		if(inhereterInheretedChanges != null)
			changesToInheret.addAll(inhereterInheretedChanges);
		List<CommandState<Model>> inhereterLocalChanges = inhereter.getLocalChanges();
		changesToInheret.addAll(inhereterLocalChanges);

		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));

		instance.playThenReverse(changesToInheret, propCtx, propDistance, collector);
		instance.setProperty("Inhereted", changesToInheret, propCtx, propDistance, collector);
		
		if(inhereter instanceof CanvasModel)
			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
		
		return instance;
	}
	
	@Override
	public void setup(Model rootModel, Model modelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
//		Model inhereter = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
//		
//		Model instance = modelToSetup;
//		
//		HistoryChangeForwarder historyChangeForwarder = new HistoryChangeForwarder(inhereter, instance);
//		inhereter.addObserver(historyChangeForwarder);
//		instance.addObserver(historyChangeForwarder);
//		historyChangeForwarder.attach(propCtx, propDistance, collector);
//		
//		ArrayList<CommandState<Model>> changesToInheret = new ArrayList<CommandState<Model>>();
//		@SuppressWarnings("unchecked")
//		List<CommandState<Model>> inhereterInheretedChanges = (List<CommandState<Model>>)inhereter.getProperty("Inhereted");
//		if(inhereterInheretedChanges != null)
//			changesToInheret.addAll(inhereterInheretedChanges);
//		List<CommandState<Model>> inhereterLocalChanges = inhereter.getLocalChanges();
//		changesToInheret.addAll(inhereterLocalChanges);
//
//		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
//		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
//		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
//		changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
//
//		instance.playThenReverse(changesToInheret, propCtx, propDistance, collector);
////		instance.playForwards2(changesToInheret, propCtx, propDistance, collector);
//		instance.setProperty("Inhereted", changesToInheret, propCtx, propDistance, collector);
//		
//		if(inhereter instanceof CanvasModel)
//			forwardHistoryChangesToContainedModels((CanvasModel)inhereter, (CanvasModel)instance, propCtx, propDistance, collector);
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
