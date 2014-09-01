package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;
import dynamake.transcription.NullTransactionHandler;
import dynamake.transcription.Trigger;

public class EnsureForwardLocalChangesUpwardsCommand implements Command<Model>, MappableCommand<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location<Model> locationOfSourceFromTarget;

	public EnsureForwardLocalChangesUpwardsCommand(Location<Model> locationOfSourceFromTarget) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		Model target = location.getChild(prevalentSystem);
		final Model source = locationOfSourceFromTarget.getChild(target);
				
		// Setup local changes upwarder in source if not already part of creation
		List<PURCommand<Model>> sourceCreation = (List<PURCommand<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		boolean changeUpwarderIsSetup = false;

		if(sourceCreation != null) {
			changeUpwarderIsSetup = sourceCreation.contains(new ForwardLocalChangesUpwardsCommand());
			
			for(PURCommand<Model> creationPart: sourceCreation) {
				if(creationPart instanceof TriStatePURCommand) {
					TriStatePURCommand<Model> pcsCreationPart = (TriStatePURCommand<Model>)creationPart;
					ReversibleCommand<Model> creationPartPending = pcsCreationPart.getPending();
					
					if(creationPartPending instanceof ReversibleCommandPair) {
						ReversibleCommandPair<Model> creationPartPendingPair = (ReversibleCommandPair<Model>)creationPartPending;
						if(creationPartPendingPair.getForth() instanceof ForwardLocalChangesUpwardsCommand) {
							changeUpwarderIsSetup = true;
							break;
						}
					}
				}
			}
		} else {
			// Set creation on source
			collector.startTransaction(source, NullTransactionHandler.class);
			
//			collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
//				new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, new ArrayList<Execution<Model>>()), 
//				new SetPropertyCommand.AfterSetProperty()
//			)));
			
			collector.execute(new TriStatePURCommand<Model>(
				new CommandSequence<Model>(
					collector.createProduceCommand(RestorableModel.PROPERTY_CREATION),
					collector.createProduceCommand(new ArrayList<PURCommand<Model>>()),
					new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
				), 
				new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
				new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
			));
			
			collector.commitTransaction();
		}
		
		if(!changeUpwarderIsSetup) {
			// Setup forwarding
			final ArrayList<PURCommand<Model>> creationForwardingUpwards = new ArrayList<PURCommand<Model>>();
			
//			creationForwardingUpwards.add(new PendingCommandState<Model>(
//				new ForwardLocalChangesUpwardsCommand(), 
//				(CommandFactory<Model>)null
//			));
			
			creationForwardingUpwards.add(new TriStatePURCommand<Model>(
				new ReversibleCommandPair<Model>(new ForwardLocalChangesUpwardsCommand(), new Command.Null<Model>()),
				new ReversibleCommandPair<Model>(new Command.Null<Model>(), new Command.Null<Model>()),
				new ReversibleCommandPair<Model>(new Command.Null<Model>(), new Command.Null<Model>())
			));
			
			collector.startTransaction(source, NullTransactionHandler.class);
			
			collector.execute(creationForwardingUpwards);
			
			collector.execute(new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					List<Object> sourceCreation = (List<Object>)source.getProperty(RestorableModel.PROPERTY_CREATION);
					
					sourceCreation.addAll(creationForwardingUpwards);
					
					// Update creation on source
					collector.execute(new TriStatePURCommand<Model>(
						new CommandSequence<Model>(
							collector.createProduceCommand(RestorableModel.PROPERTY_CREATION),
							collector.createProduceCommand(sourceCreation),
							new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
						), 
						new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
						new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
					));
				}
			});
			
			
			
//			PendingCommandFactory.Util.executeSequence(collector, creationForwardingUpwards, new ExecutionsHandler<Model>() {
//				@Override
//				public void handleExecutions(List<Execution<Model>> sourceCreationPendingUndoablePairs, Collector<Model> collector) {
//					List<Execution<Model>> sourceCreation = (List<Execution<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
//					
//					sourceCreation.addAll(sourceCreationPendingUndoablePairs);
//					
//					// Update creation on source
//					collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
//						new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, sourceCreation), 
//						new SetPropertyCommand.AfterSetProperty()
//					)));
//				}
//			});
			
			collector.commitTransaction();
		}
		
		return null;
	}
	
	@Override
	public Command<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
		Model source = CompositeLocation.getChild(sourceReference, new ModelRootLocation<Model>(), locationOfSourceFromTarget);
		Location<Model> locationOfSourceFromTargetReference = ModelComponent.Util.locationBetween(targetReference, source);
		
		return new EnsureForwardLocalChangesUpwardsCommand(locationOfSourceFromTargetReference);
	}
}
