package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.commands.PendingCommandState;
import dynamake.commands.SetPropertyCommand;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.tools.InteractionPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.LocalHistoryHandler;
import dynamake.transcription.Trigger;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	private Connection<Model> connection; 
	private InteractionPresenter interactionPresenter;
	
	public ViewDragDropPopupBuilder(Connection<Model> connection, InteractionPresenter interactionPresenter) {
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
		
		transactionTargetContentMapBuilder.addMenuBuilder("Appliance", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
				if(currentView == null)
					currentView = Model.VIEW_APPLIANCE;
				PendingCommandFactory.Util.executeSingle(collector, selection.getModelBehind(), LocalHistoryHandler.class, new PendingCommandState<Model>(
					new SetPropertyCommand(Model.PROPERTY_VIEW, Model.VIEW_APPLIANCE),
					new SetPropertyCommand(Model.PROPERTY_VIEW, currentView)
				));
			}
		});
		
		transactionTargetContentMapBuilder.addMenuBuilder("Engineering", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
				if(currentView == null)
					currentView = Model.VIEW_APPLIANCE;
				PendingCommandFactory.Util.executeSingle(collector, selection.getModelBehind(), LocalHistoryHandler.class, new PendingCommandState<Model>(
					new SetPropertyCommand(Model.PROPERTY_VIEW, Model.VIEW_ENGINEERING),
					new SetPropertyCommand(Model.PROPERTY_VIEW, currentView)
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
