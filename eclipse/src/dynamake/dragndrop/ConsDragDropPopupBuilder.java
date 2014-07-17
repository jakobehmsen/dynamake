package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.PendingCommandState;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;
import dynamake.models.Primitive;
import dynamake.models.CanvasModel.AddModelCommand2.AfterRemove;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.PrimitiveSingletonFactory;
import dynamake.tools.InteractionPresenter;
import dynamake.tools.TargetPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	private Connection<Model> connection;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;
	
	public ConsDragDropPopupBuilder(Connection<Model> connection, TargetPresenter targetPresenter, InteractionPresenter interactionPresenter) {
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
				connection.trigger(new Trigger<Model>() {
					@SuppressWarnings("unchecked")
					@Override
					public void run(Collector<Model> collector) {
						targetPresenter.reset(collector);
						interactionPresenter.reset(collector);
						
						((Trigger<Model>)action).run(collector);
						collector.commit();
					}
				});
			}
		};

		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionTargetContentMapBuilder.addMenuBuilder("Unforward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					Model.executeRemoveObserver(collector, selection, target);
				}
			});
		} else {
			transactionTargetContentMapBuilder.addMenuBuilder("Forward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					Model.executeAddObserver(collector, selection, target);
				}
			});
		}
		transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
		popup.addSeparator();
		
		CompositeMenuBuilder transactionObserverContentMapBuilder = new CompositeMenuBuilder();
		for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
			final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
			transactionObserverContentMapBuilder.addMenuBuilder(primImpl.getName(), new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
//					collector.execute(new DualCommandFactory<Model>() {
//						ModelComponent referenceMC;
//						
//						@Override
//						public Model getReference() {
//							// Common ancestor among what?
//							// The observer is not created yet
//							// Probably, it is the common ancestor among the observable and the target canvas
//							// Well, that is at least the current decision
//							referenceMC = ModelComponent.Util.closestCommonAncestor(selection, target);
//							return referenceMC.getModelBehind();
//						}
//						
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, selection);
//							ModelLocation canvasModelLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, target);
//							
//							CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
//							int index = canvasModel.getModelCount();
//							Location addedPrimitiveLocation = new CompositeModelLocation(
//								canvasModelLocation,
//								new CanvasModel.IndexLocation(index)
//							);
//							
//							// Add
//							dualCommands.add(new DualCommandPair<Model>(
//								new CanvasModel.AddModelCommand(canvasModelLocation, dropBoundsOnTarget, new PrimitiveSingletonFactory(primImpl, dropBoundsOnTarget)), 
//								new CanvasModel.RemoveModelCommand(canvasModelLocation, index)
//							));
//
//							// Bind
//							dualCommands.add(new DualCommandPair<Model>(
//								new Model.AddObserverCommand(observableLocation, addedPrimitiveLocation),
//								new Model.RemoveObserverCommand(observableLocation, addedPrimitiveLocation)
//							));
//						}
//					});
					
					collector.execute(new CommandStateFactory<Model>() {
						ModelComponent referenceMC;
						
						@Override
						public Model getReference() {
							// Common ancestor among what?
							// The observer is not created yet
							// Probably, it is the common ancestor among the observable and the target canvas
							// Well, that is at least the current decision
							referenceMC = ModelComponent.Util.closestCommonAncestor(selection, target);
							return referenceMC.getModelBehind();
						}
						
						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
							ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, selection);
							ModelLocation canvasModelLocation = ModelComponent.Util.locationFromAncestor(referenceMC, target);
							
							CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
							int index = canvasModel.getModelCount();
							Location addedPrimitiveLocation = new CompositeModelLocation(
								canvasModelLocation,
								new CanvasModel.IndexLocation(index)
							);
							
							// Add
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand2(dropBoundsOnTarget, new PrimitiveSingletonFactory(primImpl, dropBoundsOnTarget)), 
								new CanvasModel.RemoveModelCommand2.AfterAdd(),
								new CanvasModel.AddModelCommand2.AfterRemove()
							));

							// Bind
							commandStates.add(new PendingCommandState<Model>(
								new Model.AddObserverCommand2(observableLocation, addedPrimitiveLocation),
								new Model.RemoveObserverCommand2(observableLocation, addedPrimitiveLocation)
							));
						}
					});
				}
			});
		}
		transactionObserverContentMapBuilder.appendTo(popup, runner, "Observation");
	}

	@Override
	public void cancelPopup(final LivePanel livePanel) {
		connection.trigger(new Trigger<Model>() {
			public void run(Collector<Model> collector) {
				targetPresenter.reset(collector);
				interactionPresenter.reset(collector);
				
				collector.reject();
			}
		});
	}
}
