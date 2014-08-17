package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.Execution;
import dynamake.transcription.ExecutionsHandler;
import dynamake.transcription.SimplePendingCommandFactory;

public class EnsureForwardLocalChangesUpwardsCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location locationOfSourceFromTarget;

	public EnsureForwardLocalChangesUpwardsCommand(Location locationOfSourceFromTarget) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model target = (Model)location.getChild(prevalentSystem);
		final Model source = (Model)locationOfSourceFromTarget.getChild(target);
				
		// Setup local changes upwarder in source if not already part of creation
		@SuppressWarnings("unchecked")
		List<Execution<Model>> sourceCreation = (List<Execution<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		boolean changeUpwarderIsSetup = false;

		if(sourceCreation != null) {
			changeUpwarderIsSetup = sourceCreation.contains(new ForwardLocalChangesUpwardsCommand());
			
			for(Execution<Model> creationPart: sourceCreation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart.pending;

				if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesUpwardsCommand) {
					changeUpwarderIsSetup = true;
					break;
				}
			}
		} else {
			// Set creation on source
			collector.execute(new SimplePendingCommandFactory<Model>(source, new PendingCommandState<Model>(
				new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, new ArrayList<Execution<Model>>()), 
				new SetPropertyCommand.AfterSetProperty()
			)));
		}
		
		if(!changeUpwarderIsSetup) {
			// Setup forwarding
			ArrayList<CommandState<Model>> creationForwardingUpwards = new ArrayList<CommandState<Model>>();
			
			creationForwardingUpwards.add(new PendingCommandState<Model>(
				new ForwardLocalChangesUpwardsCommand(), 
				(CommandFactory<Model>)null
			));
			
			PendingCommandFactory.Util.sequence(collector, source, creationForwardingUpwards, new ExecutionsHandler<Model>() {
				@Override
				public void handleExecutions(List<Execution<Model>> sourceCreationPendingUndoablePairs, Collector<Model> collector) {
					@SuppressWarnings("unchecked")
					List<Execution<Model>> sourceCreation = (List<Execution<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
					
					sourceCreation.addAll(sourceCreationPendingUndoablePairs);
					
					// Update creation on source
					collector.execute(new SimplePendingCommandFactory<Model>(source, new PendingCommandState<Model>(
						new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, sourceCreation), 
						new SetPropertyCommand.AfterSetProperty()
					)));
				}
			});
		}
		
		return null;
	}
}
