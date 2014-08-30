package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPopupMenu;

import dynamake.commands.Command;
import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.TellPropertyCommandFromScope;
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
						collector.commitTransaction();
					}
				});
			}
		};
		
		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		transactionTargetContentMapBuilder.addMenuBuilder("Tell Color", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
//				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//					new TellPropertyCommand(Model.PROPERTY_COLOR),
//					new Command.Null<Model>()
//				));
				
				// TODO: Consider:
				// Should tell color become part of the history of the model? Or should it simply use a PostOnlyTransactionHandler?
				collector.execute(new TriStatePURCommand<Model>(
					new CommandSequence<Model>(
						collector.createProduceCommand(Model.PROPERTY_COLOR),
						new ReversibleCommandPair<Model>(new TellPropertyCommandFromScope(), new Command.Null<Model>())
					), 
					new ReversibleCommandPair<Model>(new Command.Null<Model>(), new Command.Null<Model>()),
					new CommandSequence<Model>(
						collector.createProduceCommand(Model.PROPERTY_COLOR),
						new ReversibleCommandPair<Model>(new TellPropertyCommandFromScope(), new Command.Null<Model>())
					)
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
