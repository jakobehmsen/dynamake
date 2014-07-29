package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CanvasModel;
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
		final RestorableModel restorableModelClone = RestorableModel.wrap(modelToClone, true);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				RestorableModel restorableModelCreation = restorableModelClone.mapToReferenceLocation(modelToClone, createdModel);
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
				
				@SuppressWarnings("unchecked")
				List<CommandState<Model>> changesToInheret = (List<CommandState<Model>>)createdModel.getProperty("Inhereted");

				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				changesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));

				createdModel.playThenReverse(changesToInheret, propCtx, propDistance, collector);
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model modelBase = restorableModelClone.unwrapBase();
				restorableModelClone.restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
				return modelBase;
			}
		};
	}
	
//	@Override
//	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
