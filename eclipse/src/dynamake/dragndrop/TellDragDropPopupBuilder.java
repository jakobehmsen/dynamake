package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.commands.Command2;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.PendingCommandState;
import dynamake.commands.TellPropertyCommand;
import dynamake.commands.TellPropertyCommand2;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.tools.InteractionPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	private Connection<Model> connection; 
	private InteractionPresenter interactionPresenter;
	
	public TellDragDropPopupBuilder(Connection<Model> connection, InteractionPresenter interactionPresenter) {
		this.connection = connection;
		this.interactionPresenter = interactionPresenter;
	}

	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		ActionRunner runner = new ActionRunner() {
			@Override
			public void run(final Object action) {
				connection.trigger(new Trigger<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(Collector<Model> collector) {
						interactionPresenter.reset(collector);
						
						((Trigger<Model>)action).run(collector);
						collector.commit();
					}
				});
			}
		};
		
		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		transactionTargetContentMapBuilder.addMenuBuilder("Tell Color", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
//				collector.execute(new DualCommandFactory<Model>() {
//					@Override
//					public Model getReference() {
//						return selection.getModelBehind();
//					}
//					
//					@Override
//					public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//						dualCommands.add(new DualCommandPair<Model>(
//							new TellPropertyCommand(location, Model.PROPERTY_COLOR),
//							new TellPropertyCommand(location, Model.PROPERTY_COLOR)
//						));
//					}
//				});
				
				collector.execute(new CommandStateFactory<Model>() {
					@Override
					public Model getReference() {
						return selection.getModelBehind();
					}
					
					@Override
					public void createDualCommands(List<CommandState<Model>> commandStates) {
						commandStates.add(new PendingCommandState<Model>(
							new TellPropertyCommand2(Model.PROPERTY_COLOR),
							new Command2.Null<Model>()
						));
					}
				});
			}
		});
		transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {
		connection.trigger(new Trigger<Model>() {
			public void run(Collector<Model> collector) {
				interactionPresenter.reset(collector);
				
				collector.reject();
			}
		});
	}
}
