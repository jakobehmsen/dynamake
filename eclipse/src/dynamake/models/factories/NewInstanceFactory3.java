package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.Command;
import dynamake.commands.CommandFactory;
import dynamake.commands.CommandState;
import dynamake.commands.ForwardLocalChangesCommand;
import dynamake.commands.ForwardLocalChangesUpwards2Command;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PushForwardFromCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnforwardLocalChangesCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.models.RestorableModel_TO_BE_OBSOLETED;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory2;

public class NewInstanceFactory3 implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public NewInstanceFactory3(RectangleF creationBounds, Location modelLocation) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
	}
	
	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		final Model source = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		final RestorableModel restorableModelClone = source.toRestorable(false);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, final Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model target = createdModel;
				Location locationOfSourceFromTarget = ModelComponent.Util.locationBetween(target, source);
				
				// A push forward command should forward the creation and local changes of from a source to the reference
				
				RestorableModel restorableModelCreation = restorableModelClone
					.mapToReferenceLocation(source, createdModel)
					.forForwarding();
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(
					new PushForwardFromCommand(locationOfSourceFromTarget), // Doesn't create immediate side effect
					new Command.Null<Model>() // Thus, there is no (direct) need for reverting
				));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(
					new ForwardLocalChangesCommand(locationOfSourceFromTarget), 
					new UnforwardLocalChangesCommand(locationOfSourceFromTarget)
				));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
				
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
				restorableModelCreation.restoreCleanupOnBase(createdModel, propCtx, propDistance, collector);
				
				// Setup local changes upwarder in source if not already part of creation
				@SuppressWarnings("unchecked")
				List<Model.PendingUndoablePair> sourceCreation = (List<Model.PendingUndoablePair>)source.getProperty(RestorableModel.PROPERTY_CREATION);
				boolean changeUpwarderIsSetup = false;

				if(sourceCreation != null) {
					changeUpwarderIsSetup = sourceCreation.contains(new ForwardLocalChangesUpwards2Command());
					
					for(Model.PendingUndoablePair creationPart: sourceCreation) {
						PendingCommandState<Model> pcsCreationPart = (PendingCommandState<Model>)creationPart.pending;

						if(pcsCreationPart.getCommand() instanceof ForwardLocalChangesUpwards2Command) {
							changeUpwarderIsSetup = true;
							break;
						}
					}
				} else {
					// Set creation on target
					collector.execute(new SimpleExPendingCommandFactory2<Model>(source, new PendingCommandState<Model>(
						new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, new ArrayList<Model.PendingUndoablePair>()), 
						new SetPropertyCommand.AfterSetProperty()
					)));
				}
				
				if(!changeUpwarderIsSetup) {
					// Setup forwarding
					ArrayList<CommandState<Model>> creationForwardingUpwards = new ArrayList<CommandState<Model>>();
					
					creationForwardingUpwards.add(new PendingCommandState<Model>(
						new ForwardLocalChangesUpwards2Command(), 
						(CommandFactory<Model>)null
					));
					
					collector.execute(new SimpleExPendingCommandFactory2<Model>(source, creationForwardingUpwards) {
						@Override
						public void afterPropogationFinished(List<PendingUndoablePair> sourceCreationPendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
							@SuppressWarnings("unchecked")
							List<Model.PendingUndoablePair> sourceCreation = (List<Model.PendingUndoablePair>)source.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
							
							sourceCreation.addAll(sourceCreationPendingUndoablePairs);
							
							// Update creation on source
							collector.execute(new SimpleExPendingCommandFactory2<Model>(source, new PendingCommandState<Model>(
								new SetPropertyCommand(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, sourceCreation), 
								new SetPropertyCommand.AfterSetProperty()
							)));
						}
					});
				}
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model modelBase = restorableModelClone.unwrapBase(propCtx, propDistance, collector);
				restorableModelClone.restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
				return modelBase;
			}
		};
	}
}
