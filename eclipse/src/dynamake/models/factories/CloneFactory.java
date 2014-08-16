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
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.ExPendingCommandFactory2;
import dynamake.transcription.Execution;
import dynamake.transcription.ExecutionsHandler;
import dynamake.transcription.SimpleExPendingCommandFactory;

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
		final RestorableModel restorableModelClone = modelToClone.toRestorable(true); //RestorableModel.wrap(modelToClone, true);
		
		return new ModelCreation() {
			@Override
			public void setup(Model rootModel, final Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				RestorableModel restorableModelCreation = restorableModelClone.mapToReferenceLocation(modelToClone, createdModel);
				
//				restorableModelCreation.appendCreation(creationPartToAppend)
				
//				@SuppressWarnings("unchecked")
//				List<CommandState<Model>> allCreation = (List<CommandState<Model>>)createdModel.getProperty(RestorableModel.PROPERTY_CREATION);
				
//				List<CommandState<Model>> newChangesToInheret = new ArrayList<CommandState<Model>>();

				// Same creation except that the visual position should be different
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				restorableModelCreation.appendCreation(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
				
				restorableModelCreation.restoreChangesOnBase(createdModel, propCtx, propDistance, collector);
				
//				allCreation.addAll(newChangesToInheret);

//				createdModel.playThenReverse(newChangesToInheret, propCtx, propDistance, collector);
				
//				ExPendingCommandFactory2.Util.sequence(collector, createdModel, newChangesToInheret, new ExecutionsHandler<Model>() {
//					@Override
//					public void handleExecutions(List<Execution<Model>> changesToInheritPendingUndoablePairs, Collector<Model> collector) {
//						@SuppressWarnings("unchecked")
//						List<CommandState<Model>> allCreation = (List<CommandState<Model>>)createdModel.getProperty(RestorableModel.PROPERTY_CREATION);
//						if(allCreation == null)
//							allCreation = new ArrayList<CommandState<Model>>();
//						allCreation.addAll(changesToInheritPendingUndoablePairs);
//						
//						collector.execute(new SimpleExPendingCommandFactory<Model>(createdModel, new PendingCommandState<Model>(
//							new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, allCreation), 
//							new SetPropertyCommand.AfterSetProperty()
//						)));
//					}
//				});
				
//				collector.execute(new SimpleExPendingCommandFactory<Model>(createdModel, newChangesToInheret) {
//					@Override
//					public void afterPropogationFinished(List<Execution<Model>> changesToInheritPendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//						@SuppressWarnings("unchecked")
//						List<CommandState<Model>> allCreation = (List<CommandState<Model>>)createdModel.getProperty(RestorableModel.PROPERTY_CREATION);
//						if(allCreation == null)
//							allCreation = new ArrayList<CommandState<Model>>();
//						allCreation.addAll(changesToInheritPendingUndoablePairs);
//						
//						collector.execute(new SimpleExPendingCommandFactory<Model>(createdModel, new PendingCommandState<Model>(
//							new SetPropertyCommand(RestorableModel.PROPERTY_CREATION, allCreation), 
//							new SetPropertyCommand.AfterSetProperty()
//						)));
//					}
//				});
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
