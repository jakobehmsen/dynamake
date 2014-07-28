package dynamake.models.factories;

import java.util.ArrayList;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
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
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		Model model = factory.create(rootModel, propCtx, propDistance, collector, location);
		
		ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
		
		model.setProperty("X", creationBounds.x, propCtx, propDistance, collector);
		model.setProperty("Y", creationBounds.y, propCtx, propDistance, collector);
		model.setProperty("Width", creationBounds.width, propCtx, propDistance, collector);
		model.setProperty("Height", creationBounds.height, propCtx, propDistance, collector);
		
		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("X", creationBounds.x), new SetPropertyCommand.AfterSetProperty()));
		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Y", creationBounds.y), new SetPropertyCommand.AfterSetProperty()));
		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Width", creationBounds.width), new SetPropertyCommand.AfterSetProperty()));
		origins.add(new PendingCommandState<Model>(new SetPropertyCommand("Height", creationBounds.height), new SetPropertyCommand.AfterSetProperty()));
		
//		model.setProperty("Origins", origins, propCtx, propDistance, collector);
		model.setProperty("Inhereted", origins, propCtx, propDistance, collector);
		
		return model;
	}

	@Override
	public void setup(Model rootModel, Location locationOfModelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
