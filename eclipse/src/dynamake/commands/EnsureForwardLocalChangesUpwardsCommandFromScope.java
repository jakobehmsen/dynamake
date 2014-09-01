package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;
import dynamake.transcription.NullTransactionHandler;
import dynamake.transcription.Trigger;

public class EnsureForwardLocalChangesUpwardsCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		@SuppressWarnings("unchecked")
		Location<Model> locationOfSourceFromTarget = (Location<Model>)scope.consume();
		
		Model target = location.getChild(prevalentSystem);
		final Model source = locationOfSourceFromTarget.getChild(target);
				
		// Setup local changes upwarder in source if not already part of creation
		@SuppressWarnings("unchecked")
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
					@SuppressWarnings("unchecked")
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
			
			collector.commitTransaction();
		}
		
		scope.produce(locationOfSourceFromTarget);
		
		return null;
	}
}
