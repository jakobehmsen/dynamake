package dynamake.commands;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.LocalChangesUpwarder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.ParentLocation;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnforwardLocalChangesUpwardsCommand implements MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location<Model> locationOfTarget;

	public UnforwardLocalChangesUpwardsCommand(Location<Model> locationOfSource) {
		this.locationOfTarget = locationOfSource;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		Model source = (Model)CompositeLocation.getChild(prevalentSystem, location, locationOfTarget);
		Model target = (Model)location.getChild(prevalentSystem);
		
		if(source instanceof CanvasModel)
			forwardLocalChangesUpwards((CanvasModel)source, new ModelRootLocation<Model>(), new ModelRootLocation<Model>());
		
		System.out.println("Forward local changes upwards from " + source + " to " + target);
		
		return null;
	}
	
	private void forwardLocalChangesUpwards(CanvasModel sourceCanvas, Location<Model> sourceLocation, Location<Model> offsetFromTarget) {
		for(Location<Model> modelLocationInSource: sourceCanvas.getLocations()) {
			Location<Model> modelLocationInTarget = new CanvasModel.ForwardLocation(modelLocationInSource);
			// Perhaps, the creation of this upwards forwarding should be part for play local changes from source command, for each add command?
			// - and then a corresponding cleanup for each remove command?
			Model modelInSource = sourceCanvas.getModelByLocation(modelLocationInSource);
			Location<Model> modelTargetLocation = new CompositeLocation<Model>(sourceLocation, new ParentLocation());
			Location<Model> modelOffsetFromTarget = new CompositeLocation<Model>(offsetFromTarget, modelLocationInTarget);
			modelInSource.addObserver(new LocalChangesUpwarder(modelTargetLocation, modelOffsetFromTarget));
			
			if(modelInSource instanceof CanvasModel)
				forwardLocalChangesUpwards((CanvasModel)modelInSource, modelTargetLocation, modelOffsetFromTarget);
		}
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = (Model)CompositeLocation.getChild(sourceReference, new ModelRootLocation<Model>(), locationOfTarget);
		Location<Model> locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new UnforwardLocalChangesUpwardsCommand(locationOfSourceFromTargetReference);
	}
}
