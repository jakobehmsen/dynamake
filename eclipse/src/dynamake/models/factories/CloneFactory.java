package dynamake.models.factories;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class CloneFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private Location modelLocation;
	
	public CloneFactory(RectangleF creationBounds, Location modelLocation) {
		this.creationBounds = creationBounds;
		this.modelLocation = modelLocation;
	}

	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		final Model modelToClone = (Model)CompositeLocation.getChild(rootModel, location, modelLocation);
		final RestorableModel restorableModelClone = modelToClone.toRestorable(true);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, final Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				RestorableModel restorableModelCreation = restorableModelClone.mapToReferenceLocation(modelToClone, createdModel);

				// Same creation except the visual position should be different
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
				
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location, ExecutionScope scope) {
				Model modelBase = restorableModelClone.unwrapBase(propCtx, propDistance, collector);
				restorableModelClone.restoreOriginsOnBase(modelBase, propCtx, propDistance, collector, scope);
				return modelBase;
			}
		};
	}

	@Override
	public ModelFactory forForwarding() {
		return this;
	}
}
