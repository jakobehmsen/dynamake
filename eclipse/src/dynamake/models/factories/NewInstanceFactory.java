package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.ForwardLocalChangesCommand;
import dynamake.commands.PlayLocalChangesFromSourceCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PlayThenReverseCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnforwardLocalChangesCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel_TO_BE_OBSOLETED;
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
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		final Model source = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model target = createdModel;
				
				pushCreation(source, target, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				final Model target = source.cloneBase();
				
				pushOrigins(source, target, propCtx, propDistance, collector);
				
				return target;
			}
		};
	}
	
	private void pushOrigins(Model source, Model target, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> origins = (List<CommandState<Model>>)source.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_ORIGINS);
		
		target.playThenReverse(origins, propCtx, propDistance, collector);
		target.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_ORIGINS, origins, propCtx, propDistance, collector);
	}
	
	private void pushCreation(Model source, Model target, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> creation = (List<CommandState<Model>>)source.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
		
		ArrayList<CommandState<Model>> newCreation = new ArrayList<CommandState<Model>>();
		
		if(creation != null) {
			int rootDistanceFromReference = 1;
			
			for(CommandState<Model> creationPart: creation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart;
				if(pcsCreationPart.getCommand() instanceof PlayLocalChangesFromSourceCommand)
					rootDistanceFromReference++;
			}
			
			int playCommandCount = 0;
			// Each PlayLocalChangesFromSourceCommand should have its distance incremented by one
			
			// Don't include ForwardHistoryCommand commands in changes to inheret 
			// TODO: Remove the ugly filter hack below; replace with decoupled logic
			for(CommandState<Model> creationPart: creation) {
				PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart;
				
				if(pcsCreationPart.getCommand() instanceof PlayLocalChangesFromSourceCommand) {
					PlayLocalChangesFromSourceCommand playCommand = (PlayLocalChangesFromSourceCommand)pcsCreationPart.getCommand();
					
					int rootDistance = rootDistanceFromReference - playCommandCount;
					playCommand = playCommand.whereRootDistanceIs(rootDistance);
					
					pcsCreationPart = new PendingCommandState<Model>(playCommand, pcsCreationPart.getForthFactory(), pcsCreationPart.getForthFactory());
					
					playCommandCount++;
				}
				
				if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesCommand) {

				} else
					newCreation.add(pcsCreationPart.mapToReferenceLocation(source, target));
			}
		}
		
//		// Setup forwarding
//		ArrayList<CommandState<Model>> creationForwarding = new ArrayList<CommandState<Model>>();
//		
//		Location locationOfSourceFromTarget = ModelComponent.Util.locationBetween(target, source);
//		
//		creationForwarding.add(new PendingCommandState<Model>(
//			new ForwardLocalChangesCommand(locationOfSourceFromTarget), 
//			new UnforwardLocalChangesCommand(locationOfSourceFromTarget)
//		));
//
//		// TODO: Consider: Inherit cleanup?
//		List<CommandState<Model>> cleanup = target.playThenReverse(creationForwarding, propCtx, propDistance, collector);
//		target.setProperty(RestorableModel.PROPERTY_CLEANUP, cleanup, propCtx, propDistance, collector);
		
		target.playThenReverse(newCreation, propCtx, propDistance, collector);
		
//		newCreation.addAll(creationForwarding);

		Location locationOfSourceFromTarget = ModelComponent.Util.locationBetween(target, source);
		
		ArrayList<CommandState<Model>> newCreationLastParts = new ArrayList<CommandState<Model>>();
		
		newCreationLastParts.add(new PendingCommandState<Model>(new PlayLocalChangesFromSourceCommand(locationOfSourceFromTarget), new PlayThenReverseCommand.AfterPlay()));
		
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
		newCreationLastParts.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
		
		target.playThenReverse(newCreationLastParts, propCtx, propDistance, collector);
		newCreation.addAll(newCreationLastParts);
		
		
		
		// Setup forwarding
		ArrayList<CommandState<Model>> creationForwarding = new ArrayList<CommandState<Model>>();
		
		creationForwarding.add(new PendingCommandState<Model>(
			new ForwardLocalChangesCommand(locationOfSourceFromTarget), 
			new UnforwardLocalChangesCommand(locationOfSourceFromTarget)
		));

		// TODO: Consider: Inherit cleanup?
		List<CommandState<Model>> cleanup = target.playThenReverse(creationForwarding, propCtx, propDistance, collector);
		target.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CLEANUP, cleanup, propCtx, propDistance, collector);
		
		newCreation.addAll(creationForwarding);
		
		
		
		target.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, newCreation, propCtx, propDistance, collector);
	}
}
