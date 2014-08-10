package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory;
import dynamake.transcription.Trigger;

public class PushForwardFromCommand implements ForwardableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location locationOfSourceFromTarget;
	private int forwardCount;
	
	public PushForwardFromCommand(Location locationOfSourceFromTarget) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
		this.forwardCount = 1;
	}
	
	private PushForwardFromCommand(Location locationOfSourceFromTarget, int forwardCount) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
		this.forwardCount = forwardCount;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model target = (Model)location.getChild(prevalentSystem);
		Model source = (Model)locationOfSourceFromTarget.getChild(target);
		
		pushForward(source, target, collector, new ModelRootLocation());
		
//		ArrayList<CommandState<Model>> toForward = new ArrayList<CommandState<Model>>();
//		
//		@SuppressWarnings("unchecked")
//		List<CommandState<Model>> sourceCreation = (List<CommandState<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
//		
//		if(sourceCreation != null) {
//			toForward.addAll(sourceCreation);
//		}
//		
//		toForward.addAll(source.getLocalChanges());
//		
//		if(toForward.size() > 0) {
//			ArrayList<CommandState<Model>> forwardedCreation = new ArrayList<CommandState<Model>>();
//			for(CommandState<Model> creationPart: toForward) {
//				creationPart = creationPart.mapToReferenceLocation(source, target);
//				for(int i = 0; i < forwardCount; i++)
//					creationPart = creationPart.forForwarding();
//				creationPart.appendPendings(forwardedCreation);
//			}
//			
//			collector.execute(new SimpleExPendingCommandFactory2<Model>(target, forwardedCreation));
//		}

		return null;
	}
	
	private void pushForward(Model source, final Model targetRoot, Collector<Model> collector, final Location offsetFromSourceRoot) {		
		ArrayList<CommandState<Model>> toForward = new ArrayList<CommandState<Model>>();
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> sourceCreation = (List<CommandState<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		
		if(sourceCreation != null) {
			toForward.addAll(sourceCreation);
		}
		
		toForward.addAll(source.getLocalChanges());
		
		if(toForward.size() > 0) {
			Location forwardedOffsetFromTargetRoot = offsetFromSourceRoot;
			for(int i = 0; i < forwardCount; i++)
				forwardedOffsetFromTargetRoot = forwardedOffsetFromTargetRoot.forForwarding();
			
			ArrayList<CommandState<Model>> forwardedCreation = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> creationPart: toForward) {
				creationPart = creationPart.mapToReferenceLocation(source, targetRoot);
				for(int i = 0; i < forwardCount; i++)
					creationPart = creationPart.forForwarding();
				creationPart = creationPart.offset(forwardedOffsetFromTargetRoot);
				creationPart.appendPendings(forwardedCreation);
			}
			
			collector.execute(new SimpleExPendingCommandFactory<Model>(targetRoot, forwardedCreation));
		}
		
		if(source instanceof CanvasModel) {
			final CanvasModel sourceCanvas = (CanvasModel)source;

			for(final Location locationInSource: sourceCanvas.getLocations()) {
				collector.execute(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						Model modelInSource = sourceCanvas.getModelByLocation(locationInSource);
						Location newOffsetFromSourceRoot = new CompositeLocation(offsetFromSourceRoot, locationInSource);
						
						pushForward(modelInSource, targetRoot, collector, newOffsetFromSourceRoot);
					}
				});
			}
		}
	}
	
	@Override
	public Command<Model> forForwarding(Object output) {
		return new PushForwardFromCommand(locationOfSourceFromTarget, forwardCount + 1);
	}
}
