package dynamake.models.factories;

import dynamake.commands.Command;
import dynamake.commands.CommandSequence;
import dynamake.commands.EnsureForwardLocalChangesUpwardsCommandFromScope;
import dynamake.commands.ExecutionScope;
import dynamake.commands.ForwardLocalChangesCommandFromScope;
import dynamake.commands.IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommandFromScope;
import dynamake.commands.PushForwardFromCommandFromScope;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.commands.UnforwardLocalChangesCommandFromScope;
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
	private Location<Model> modelLocation;
	private boolean forwarded;
	
	public DeriveFactory(RectangleF creationBounds, Location<Model> modelLocation) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
	}
	
	public DeriveFactory(RectangleF creationBounds, Location<Model> modelLocation, boolean forwarded) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
		this.forwarded = forwarded;
	}
	
	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) {
		final Model source = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		final RestorableModel restorableModelClone = source.toRestorable(false);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, final Model createdModel, Location<Model> locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) {
				Model target = createdModel;
				Location<Model> locationOfSourceFromTarget = ModelComponent.Util.locationBetween(target, source);
				
				// A push forward command should forward the creation and local changes of from a source to the reference
				RestorableModel restorableModelCreation = restorableModelClone.forForwarding();
					
				restorableModelCreation.clearCreation();
//				restorableModelCreation.appendCreation(new PendingCommandState<Model>(
//					new PushForwardFromCommand(locationOfSourceFromTarget), // Doesn't create immediate side effect
//					new Command.Null<Model>() // Thus, there is no (direct) need for reverting
//				));
				
				restorableModelCreation.appendCreation(new TriStatePURCommand<Model>(
					new CommandSequence<Model>(
						collector.createProduceCommand(locationOfSourceFromTarget),
						new ReversibleCommandPair<Model>(new PushForwardFromCommandFromScope(), new  Command.Null<Model>())
					), 
					new ReversibleCommandPair<Model>(new Command.Null<Model>(), new PushForwardFromCommandFromScope()),
					new ReversibleCommandPair<Model>(new PushForwardFromCommandFromScope(), new Command.Null<Model>())
				));
				
				if(!forwarded) {
//					restorableModelCreation.appendCreation(new PendingCommandState<Model>(
//						new ForwardLocalChangesCommand(locationOfSourceFromTarget), 
//						new UnforwardLocalChangesCommand(locationOfSourceFromTarget)
//					));

					restorableModelCreation.appendCreation(new TriStatePURCommand<Model>(
						new CommandSequence<Model>(
							collector.createProduceCommand(locationOfSourceFromTarget),
							new ReversibleCommandPair<Model>(new ForwardLocalChangesCommandFromScope(), new UnforwardLocalChangesCommandFromScope())
						), 
						new ReversibleCommandPair<Model>(new UnforwardLocalChangesCommandFromScope(), new ForwardLocalChangesCommandFromScope()),
						new ReversibleCommandPair<Model>(new ForwardLocalChangesCommandFromScope(), new UnforwardLocalChangesCommandFromScope())
					));
				}
				
				restorableModelCreation.appendCreation(SetPropertyCommandFromScope.createPURCommand(collector, "X", creationBounds.x));
				restorableModelCreation.appendCreation(SetPropertyCommandFromScope.createPURCommand(collector, "Y", creationBounds.y));
				restorableModelCreation.appendCreation(SetPropertyCommandFromScope.createPURCommand(collector, "Width", creationBounds.width));
				restorableModelCreation.appendCreation(SetPropertyCommandFromScope.createPURCommand(collector, "Height", creationBounds.height));
				
				if(!forwarded) {
//					restorableModelCreation.appendCreation(new PendingCommandState<Model>(
//						new EnsureForwardLocalChangesUpwardsCommand(locationOfSourceFromTarget), 
//						new IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommand(locationOfSourceFromTarget)
//					));
					restorableModelCreation.appendCreation(new TriStatePURCommand<Model>(
						new CommandSequence<Model>(
							collector.createProduceCommand(locationOfSourceFromTarget),
							new ReversibleCommandPair<Model>(new EnsureForwardLocalChangesUpwardsCommandFromScope(), new IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommandFromScope())
						), 
						new ReversibleCommandPair<Model>(new IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommandFromScope(), new EnsureForwardLocalChangesUpwardsCommandFromScope()),
						new ReversibleCommandPair<Model>(new EnsureForwardLocalChangesUpwardsCommandFromScope(), new IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommandFromScope())
					));
				}
				
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
				Model modelBase = restorableModelClone.unwrapBase(propCtx, propDistance, collector);
				restorableModelClone.restoreOriginsOnBase(modelBase, propCtx, propDistance, collector, scope);
				return modelBase;
			}
		};
	}

	@Override
	public ModelFactory forForwarding() {
		// When a derivation is upwarded, then it most it is forwarded into another derivation with is already setup with a forwarder.
		return new DeriveFactory(creationBounds, modelLocation, true);
	}
}
