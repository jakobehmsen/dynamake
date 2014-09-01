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
import dynamake.transcription.NullTransactionHandler;
import dynamake.transcription.Trigger;

public class PushForwardFromCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		@SuppressWarnings("unchecked")
		Location<Model> locationOfSourceFromTarget = (Location<Model>)scope.consume();
		// Somehow, forwardCount should be forwardable, such that each time forwardCount is forwarded, it is incremented by one
		int forwardCount = (int)scope.consume();
		
		Model target = location.getChild(prevalentSystem);
		Model source = locationOfSourceFromTarget.getChild(target);
		
		collector.startTransaction(target, NullTransactionHandler.class);
		pushForward(forwardCount, source, target, collector, new ModelRootLocation<Model>());
		collector.commitTransaction();
		
		scope.produce(forwardCount);
		scope.produce(locationOfSourceFromTarget);

		return null;
	}
	
	private void pushForward(final int forwardCount, Model source, final Model targetRoot, Collector<Model> collector, final Location<Model> offsetFromSourceRoot) {		
		ArrayList<PURCommand<Model>> toForward = new ArrayList<PURCommand<Model>>();
		
		@SuppressWarnings("unchecked")
		List<PURCommand<Model>> sourceCreation = (List<PURCommand<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		
		if(sourceCreation != null) {
			toForward.addAll(sourceCreation);
		}
		
		toForward.addAll(source.getLocalChanges());
		
		if(toForward.size() > 0) {
			Location<Model> forwardedOffsetFromTargetRoot = offsetFromSourceRoot;
			for(int i = 0; i < forwardCount; i++)
				forwardedOffsetFromTargetRoot = forwardedOffsetFromTargetRoot.forForwarding();
			
			ArrayList<PURCommand<Model>> forwardedCreation = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> creationPart: toForward) {
				creationPart = (PURCommand<Model>) creationPart.mapToReferenceLocation(source, targetRoot);
				for(int i = 0; i < forwardCount; i++)
					creationPart = (PURCommand<Model>) creationPart.forForwarding();
//				creationPart = creationPart.offset(forwardedOffsetFromTargetRoot);
				
//				creationPart.appendPendings(forwardedCreation);
				forwardedCreation.add(creationPart.inReplayState());
			}
			
			collector.execute(collector.createProduceCommand(forwardedOffsetFromTargetRoot));
			collector.execute(collector.createPushOffset());
			
//			PendingCommandFactory.Util.executeSequence(collector, forwardedCreation);
			collector.execute(forwardedCreation);
			
			collector.execute(collector.createPopOffset());
		}
		
		if(source instanceof CanvasModel) {
			final CanvasModel sourceCanvas = (CanvasModel)source;

			for(final Location<Model> locationInSource: sourceCanvas.getLocations()) {
				collector.execute(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						Model modelInSource = sourceCanvas.getModelByLocation(locationInSource);
						Location<Model> newOffsetFromSourceRoot = new CompositeLocation<Model>(offsetFromSourceRoot, locationInSource);
						
						pushForward(forwardCount, modelInSource, targetRoot, collector, newOffsetFromSourceRoot);
					}
				});
			}
		}
	}
}
