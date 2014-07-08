package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.tools.InteractionPresenter;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberRunnable;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	private TranscriberConnection<Model> connection; 
	private InteractionPresenter interactionPresenter;
	
	public ViewDragDropPopupBuilder(TranscriberConnection<Model> connection, InteractionPresenter interactionPresenter) {
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
				connection.trigger(new TranscriberRunnable<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(TranscriberCollector<Model> collector) {
						interactionPresenter.reset(collector);
						
						((TranscriberRunnable<Model>)action).run(collector);
						collector.enlistCommit();
						collector.flush();
					}
				});
			}
		};
		
		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		transactionTargetContentMapBuilder.addMenuBuilder("Appliance", new TranscriberRunnable<Model>() {
			@Override
			public void run(TranscriberCollector<Model> collector) {
				collector.execute(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_APPLIANCE),
							new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));
					}
				});
			}
		});
		
		transactionTargetContentMapBuilder.addMenuBuilder("Engineering", new TranscriberRunnable<Model>() {
			@Override
			public void run(TranscriberCollector<Model> collector) {
				collector.execute(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_ENGINEERING),
							new Model.SetPropertyTransaction(selection.getModelTranscriber().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));
					}
				});
			}
		});
		
		transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {
		connection.trigger(new TranscriberRunnable<Model>() {
			public void run(TranscriberCollector<Model> collector) {
				interactionPresenter.reset(collector);
				
				collector.enlistReject();
				collector.flush();
			}
		});
	}
}
