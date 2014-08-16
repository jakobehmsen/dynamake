package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.TellPropertyCommand;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.CreationBoundsFactory;
import dynamake.models.factories.ModelFactory;
import dynamake.models.factories.StrokeModelFactory;
import dynamake.numbers.RectangleF;
import dynamake.tools.InteractionPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.ExPendingCommandFactory2;
import dynamake.transcription.LocalHistoryHandler;
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
				ExPendingCommandFactory2.Util.single(collector, selection.getModelBehind(), LocalHistoryHandler.class, new PendingCommandState<Model>(
					new TellPropertyCommand(Model.PROPERTY_COLOR),
					new Command.Null<Model>()
				));
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
