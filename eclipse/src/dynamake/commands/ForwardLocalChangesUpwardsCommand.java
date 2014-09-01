package dynamake.commands;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesUpwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelRootLocation;
import dynamake.models.ParentLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class ForwardLocalChangesUpwardsCommand implements ForwardableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		Model target = location.getChild(prevalentSystem);
		
		// The relation should also be maintained for new models created
		// Thus, some sort of observer (ForwardLocalChangesObserver) should be added for each of the contained models
		
		target.addObserver(new LocalChangesUpwarder(new ModelRootLocation<Model>(), new ModelRootLocation<Model>()));
		
		if(target instanceof CanvasModel)
			forwardLocalChangesUpwards((CanvasModel)target, new ModelRootLocation<Model>(), new ModelRootLocation<Model>());
		
		System.out.println("Forward local changes upwards from " + target);
		
		return null;
	}
	
	private void forwardLocalChangesUpwards(CanvasModel targetCanvas, Location<Model> sourceLocation, Location<Model> offsetFromTarget) {
		for(Location<Model> modelLocationInSource: targetCanvas.getLocations()) {
			// Perhaps, the creation of this upwards forwarding should be part for play local changes from source command, for each add command?
			// - and then a corresponding cleanup for each remove command?
			Model modelInTarget = targetCanvas.getModelByLocation(modelLocationInSource);
			Location<Model> modelTargetLocation = new CompositeLocation<Model>(sourceLocation, new ParentLocation());
			Location<Model> modelOffsetFromTarget = new CompositeLocation<Model>(offsetFromTarget, modelLocationInSource);
			modelInTarget.addObserver(new LocalChangesUpwarder(modelTargetLocation, modelOffsetFromTarget));
			
			if(modelInTarget instanceof CanvasModel)
				forwardLocalChangesUpwards((CanvasModel)modelInTarget, modelTargetLocation, modelOffsetFromTarget);
		}
	}

	@Override
	public Command<Model> forForwarding(Object output) {
		return new Command.Null<Model>();
	}
}
