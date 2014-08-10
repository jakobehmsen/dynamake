package dynamake.models.factories;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory2;

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
			public void setup(Model rootModel, final Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				RestorableModel restorableModelCreation = restorableModelClone.mapToReferenceLocation(modelToClone, createdModel);
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
				restorableModelCreation.restoreCleanupOnBase(createdModel, propCtx, propDistance, collector);
				
//				@SuppressWarnings("unchecked")
//				List<CommandState<Model>> allCreation = (List<CommandState<Model>>)createdModel.getProperty(RestorableModel.PROPERTY_CREATION);
				
				List<CommandState<Model>> newChangesToInheret = new ArrayList<CommandState<Model>>();

				newChangesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				newChangesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				newChangesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				newChangesToInheret.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
				
//				allCreation.addAll(newChangesToInheret);

//				createdModel.playThenReverse(newChangesToInheret, propCtx, propDistance, collector);
				
				collector.execute(new SimpleExPendingCommandFactory2<Model>(createdModel, newChangesToInheret) {
					@Override
					public void afterPropogationFinished(List<PendingUndoablePair> changesToInheritPendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
						@SuppressWarnings("unchecked")
						List<CommandState<Model>> allCreation = (List<CommandState<Model>>)createdModel.getProperty(RestorableModel.PROPERTY_CREATION);
						if(allCreation == null)
							allCreation = new ArrayList<CommandState<Model>>();
						allCreation.addAll(changesToInheritPendingUndoablePairs);
						
						collector.execute(new SimpleExPendingCommandFactory2<Model>(createdModel, new PendingCommandState<Model>(
							new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, allCreation), 
							new SetPropertyCommand.AfterSetProperty()
						)));
					}
				});
			}
			
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model modelBase = restorableModelClone.unwrapBase(propCtx, propDistance, collector);
				restorableModelClone.restoreOriginsOnBase(modelBase, propCtx, propDistance, collector);
				return modelBase;
			}
		};
	}
	
//	@Override
//	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
