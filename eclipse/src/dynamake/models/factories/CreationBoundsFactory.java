package dynamake.models.factories;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel_TO_BE_OBSOLETED;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;

public class CreationBoundsFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private RectangleF creationBounds;
	private ModelFactory factory;

	public CreationBoundsFactory(RectangleF creationBounds, ModelFactory factory) {
		this.creationBounds = creationBounds;
		this.factory = factory;
	}

	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		final ModelCreation modelCreation = factory.create(rootModel, propCtx, propDistance, collector, location);
		
		return new ModelCreation() {
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				Model model = modelCreation.createModel(rootModel, propCtx, propDistance, collector, location);

				ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
				
//				model.setProperty("X", creationBounds.x, propCtx, propDistance, collector);
//				model.setProperty("Y", creationBounds.y, propCtx, propDistance, collector);
//				model.setProperty("Width", creationBounds.width, propCtx, propDistance, collector);
//				model.setProperty("Height", creationBounds.height, propCtx, propDistance, collector);
				
				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
				
				model.playThenReverse(origins, propCtx, propDistance, collector);
				
				model.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_ORIGINS, origins, propCtx, propDistance, collector);
//				model.setProperty(RestorableModel.PROPERTY_CREATION, origins, propCtx, propDistance, collector);
				
				return model;
			}

			@Override
			public void setup(Model rootModel, Model createdModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
				modelCreation.setup(rootModel, createdModel, locationOfModelToSetup, propCtx, propDistance, collector, location);
			}
		};
		
//		ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
//		
//		model.setProperty("X", creationBounds.x, propCtx, propDistance, collector);
//		model.setProperty("Y", creationBounds.y, propCtx, propDistance, collector);
//		model.setProperty("Width", creationBounds.width, propCtx, propDistance, collector);
//		model.setProperty("Height", creationBounds.height, propCtx, propDistance, collector);
//		
//		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
//		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
//		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
//		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
//		
////		model.setProperty(RestorableModel.PROPERTY_ORIGINS, origins, propCtx, propDistance, collector);
//		model.setProperty(RestorableModel.PROPERTY_CREATION, origins, propCtx, propDistance, collector);
//		
//		return model;
	}

//	@Override
//	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
