package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.tools.InteractionPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
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
						collector.commitTransaction();
					}
				});
			}
		};
		
		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		transactionTargetContentMapBuilder.addMenuBuilder("Appliance", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
//				Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
//				if(currentView == null)
//					currentView = Model.VIEW_APPLIANCE;
//				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//					new SetPropertyCommand(Model.PROPERTY_VIEW, Model.VIEW_APPLIANCE),
//					new SetPropertyCommand(Model.PROPERTY_VIEW, currentView)
//				));
				
				collector.execute(new TriStatePURCommand<Model>(
					new CommandSequence<Model>(
						collector.createProduceCommand(Model.PROPERTY_VIEW),
						collector.createProduceCommand(Model.VIEW_APPLIANCE),
						new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
					), 
					new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()), // Outputs name of changed property and the previous value
					new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
				));
			}
		});
		
		transactionTargetContentMapBuilder.addMenuBuilder("Engineering", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
//				Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
//				if(currentView == null)
//					currentView = Model.VIEW_APPLIANCE;
//				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//					new SetPropertyCommand(Model.PROPERTY_VIEW, Model.VIEW_ENGINEERING),
//					new SetPropertyCommand(Model.PROPERTY_VIEW, currentView)
//				));
				
				collector.execute(new TriStatePURCommand<Model>(
					new CommandSequence<Model>(
						collector.createProduceCommand(Model.PROPERTY_VIEW),
						collector.createProduceCommand(Model.VIEW_ENGINEERING),
						new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
					), 
					new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()), // Outputs name of changed property and the previous value
					new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
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
				
				collector.rejectTransaction();
			}
		});
	}
}
