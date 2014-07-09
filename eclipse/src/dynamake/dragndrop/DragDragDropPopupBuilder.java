package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import dynamake.commands.DejectTransaction;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.InjectTransaction;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.LivePanel;
import dynamake.tools.InteractionPresenter;
import dynamake.tools.TargetPresenter;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

public class DragDragDropPopupBuilder implements DragDropPopupBuilder {
	private Connection<Model> connection;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;
	
	public DragDragDropPopupBuilder(Connection<Model> connection, TargetPresenter targetPresenter, InteractionPresenter interactionPresenter) {
		this.connection = connection;
		this.targetPresenter = targetPresenter;
		this.interactionPresenter = interactionPresenter;
	}

	@Override
	public void buildFromSelectionAndTarget(ModelComponent livePanel,
			JPopupMenu popup, ModelComponent selection,
			ModelComponent target, Point dropPointOnTarget, Rectangle dropBoundsOnTarget) {
		if(target == null || target == selection) {
			// Build popup menu for dropping onto selection
			buildFromSelectionToSelection(livePanel, popup, selection);
		} else {
			// Build popup menu for dropping onto other
			buildFromSelectionToOther(livePanel, popup, selection, target, dropPointOnTarget, dropBoundsOnTarget);
		}
	}
	
	private void buildFromSelectionToSelection(final ModelComponent livePanel, JPopupMenu popup, ModelComponent selection) {
		ActionRunner runner = new ActionRunner() {
			@Override
			public void run(final Object action) {
				connection.trigger(new Trigger<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(Collector<Model> collector) {
						targetPresenter.reset(collector);
						interactionPresenter.reset(collector);
						
						((Trigger<Model>)action).run(collector);
						collector.enlistCommit();
					}
				});
			}
		};
		
		ModelComponent parentModelComponent = ModelComponent.Util.closestModelComponent(((JComponent)selection).getParent()); 
		
		CompositeMenuBuilder containerTransactionMapBuilder = new CompositeMenuBuilder();
		// Assume only TranscriberRunnable<Model> to be added as actions for menus
		if(parentModelComponent != null)
			parentModelComponent.appendContainerTransactions((LivePanel)livePanel, containerTransactionMapBuilder, selection);

		CompositeMenuBuilder transactionSelectionMapBuilder = new CompositeMenuBuilder();
		selection.appendTransactions(livePanel, transactionSelectionMapBuilder);

		containerTransactionMapBuilder.appendTo(popup, runner, "Container");
		if(!containerTransactionMapBuilder.isEmpty() && !transactionSelectionMapBuilder.isEmpty())
			popup.addSeparator();
		transactionSelectionMapBuilder.appendTo(popup, runner, "Selection");
	}

	private void buildFromSelectionToOther(final ModelComponent livePanel, JPopupMenu popup, final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		ActionRunner runner = new ActionRunner() {
			@Override
			public void run(final Object action) {
				connection.trigger(new Trigger<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(Collector<Model> collector) {
						targetPresenter.reset(collector);
						interactionPresenter.reset(collector);
						
						((Trigger<Model>)action).run(collector);
						collector.enlistCommit();
					}
				});
			}
		};
		
		CompositeMenuBuilder transactionSelectionGeneralMapBuilder = new CompositeMenuBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionSelectionGeneralMapBuilder.addMenuBuilder("Unforward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					collector.enlist(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getModelTranscriber().getModelLocation();
							Location observerLocation = target.getModelTranscriber().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
								new Model.AddObserver(observableLocation, observerLocation) // Absolute location
							));
						}
					});
				}
			});
		} else {
			transactionSelectionGeneralMapBuilder.addMenuBuilder("Forward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					collector.enlist(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getModelTranscriber().getModelLocation();
							Location observerLocation = target.getModelTranscriber().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.AddObserver(observableLocation, observerLocation), // Absolute location
								new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
							));
						}
					});
				}
			});
		}
		
		transactionSelectionGeneralMapBuilder.addMenuBuilder("Inject", new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				collector.enlist(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(new DualCommandPair<Model>(
							new InjectTransaction(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation()),
							new DejectTransaction(selection.getModelTranscriber().getModelLocation(), target.getModelTranscriber().getModelLocation())
						));
					}
				});
			}
		});

		CompositeMenuBuilder transactionTargetMapBuilder = new CompositeMenuBuilder();
		target.appendDropTargetTransactions(livePanel, selection, dropBoundsOnTarget, dropPointOnTarget, transactionTargetMapBuilder);
		
		transactionSelectionGeneralMapBuilder.appendTo(popup, runner, "General");
		if(!transactionSelectionGeneralMapBuilder.isEmpty() && !transactionTargetMapBuilder.isEmpty())
			popup.addSeparator();
		transactionTargetMapBuilder.appendTo(popup, runner, "Target");

		CompositeMenuBuilder transactionDroppedMapBuilder = new CompositeMenuBuilder();
		selection.appendDroppedTransactions(livePanel, target, dropBoundsOnTarget, transactionDroppedMapBuilder);
		if(!transactionTargetMapBuilder.isEmpty() && !transactionDroppedMapBuilder.isEmpty())
			popup.addSeparator();
		transactionDroppedMapBuilder.appendTo(popup, runner, "Dropped");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {
		connection.trigger(new Trigger<Model>() {
			public void run(Collector<Model> collector) {
				targetPresenter.reset(collector);
				interactionPresenter.reset(collector);
				
				collector.enlistReject();
			}
		});
	}
}
