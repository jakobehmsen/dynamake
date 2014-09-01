package dynamake.models.factories;

import java.util.ArrayList;

import dynamake.commands.ExecutionScope;
import dynamake.commands.PURCommand;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
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
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) {
		final ModelCreation modelCreation = factory.create(rootModel, propCtx, propDistance, collector, location);
		
		return new ModelCreation() {
			@Override
			public Model createModel(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
				Model model = modelCreation.createModel(rootModel, propCtx, propDistance, collector, location, scope);

				ArrayList<PURCommand<Model>> origins = new ArrayList<PURCommand<Model>>();
				
//				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
//				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
//				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
//				origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));				
				
				origins.add(SetPropertyCommandFromScope.createPURCommand(collector, "X", creationBounds.x));
				origins.add(SetPropertyCommandFromScope.createPURCommand(collector, "Y", creationBounds.y));
				origins.add(SetPropertyCommandFromScope.createPURCommand(collector, "Width", creationBounds.width));
				origins.add(SetPropertyCommandFromScope.createPURCommand(collector, "Height", creationBounds.height));
				
				model.playThenReverse(origins, propCtx, propDistance, collector, scope);
				
				model.setProperty(RestorableModel.PROPERTY_ORIGINS, origins, propCtx, propDistance, collector);
				
				return model;
			}

			@Override
			public void setup(Model rootModel, Model createdModel, Location<Model> locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location<Model> location) {
				modelCreation.setup(rootModel, createdModel, locationOfModelToSetup, propCtx, propDistance, collector, location);
			}
		};
	}

	@Override
	public ModelFactory forForwarding() {
		return this;
	}
}
