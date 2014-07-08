package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.Primitive;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.PrimitiveSingletonFactory;
import dynamake.tools.InteractionPresenter;
import dynamake.tools.TargetPresenter;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberRunnable;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	private TranscriberConnection<Model> connection;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;
	
	public ConsDragDropPopupBuilder(TranscriberConnection<Model> connection, TargetPresenter targetPresenter, InteractionPresenter interactionPresenter) {
		this.connection = connection;
		this.targetPresenter = targetPresenter;
		this.interactionPresenter = interactionPresenter;
	}

	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		ActionRunner runner = new ActionRunner() {
			@Override
			public void run(final Object action) {
				connection.trigger(new TranscriberRunnable<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(TranscriberCollector<Model> collector) {
						targetPresenter.reset(collector);
						interactionPresenter.reset(collector);
						
						((TranscriberRunnable<Model>)action).run(collector);
						collector.enlistCommit();
						collector.flush();
					}
				});
			}
		};
		
		final DualCommandFactory<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			connection.trigger(new TranscriberRunnable<Model>() {
				@Override
				public void run(TranscriberCollector<Model> collector) {
					collector.enlist(implicitDropAction);
					collector.enlistCommit();
				}
			});
		} else {
			CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addMenuBuilder("Unforward to", new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.enlist(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new Model.RemoveObserver(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation()),
									new Model.AddObserver(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation())
								));
							}
						});
					}
				});
			} else {
				transactionTargetContentMapBuilder.addMenuBuilder("Forward to", new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.enlist(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new Model.AddObserver(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation()),
									new Model.RemoveObserver(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation())
								));
							}
						});
					}
				});
			}
			transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
			popup.addSeparator();
			
			CompositeMenuBuilder transactionObserverContentMapBuilder = new CompositeMenuBuilder();
			for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
				final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
				transactionObserverContentMapBuilder.addMenuBuilder(primImpl.getName(), new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.enlist(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
								Location canvasModelLocation = target.getModelTranscriber().getModelLocation();
								int index = canvasModel.getModelCount();
								Location addedPrimitiveLocation = target.getModelTranscriber().extendLocation(new CanvasModel.IndexLocation(index));
								// The location for Bind and Output depends on the side effect of add
								
								// Add
								dualCommands.add(new DualCommandPair<Model>(
									new CanvasModel.AddModelTransaction(canvasModelLocation, dropBoundsOnTarget, new PrimitiveSingletonFactory(primImpl)), 
									new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
								));

								// Bind
								dualCommands.add(new DualCommandPair<Model>(
									new Model.AddObserver(selection.getModelTranscriber().getModelLocation(), addedPrimitiveLocation), // Absolute location
									new Model.RemoveObserver(selection.getModelTranscriber().getModelLocation(), addedPrimitiveLocation) // Absolute location
								));
							}
						});
					}
				});
			}
			transactionObserverContentMapBuilder.appendTo(popup, runner, "Observation");
		}
	}

	@Override
	public void cancelPopup(final LivePanel livePanel) {
		connection.trigger(new TranscriberRunnable<Model>() {
			public void run(TranscriberCollector<Model> collector) {
				targetPresenter.reset(collector);
				interactionPresenter.reset(collector);
				
				collector.enlistReject();
				collector.flush();
			}
		});
	}
}
