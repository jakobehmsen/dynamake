package dynamake.models.factories;

import dynamake.commands.Command;
import dynamake.commands.EnsureForwardLocalChangesUpwardsCommand;
import dynamake.commands.ForwardLocalChangesCommand;
import dynamake.commands.IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.PushForwardFromCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.commands.UnforwardLocalChangesCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class DeriveFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public DeriveFactory(RectangleF creationBounds, Location modelLocation) {
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
				RestorableModel restorableModelCreation = restorableModelClone.forForwarding();
					
				restorableModelCreation.clearCreation();
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
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(
					new EnsureForwardLocalChangesUpwardsCommand(locationOfSourceFromTarget), 
					new IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommand(locationOfSourceFromTarget)
				));
				
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
				restorableModelCreation.restoreCleanupOnBase(createdModel, propCtx, propDistance, collector);
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
