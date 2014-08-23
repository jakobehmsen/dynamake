package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import dynamake.commands.AddObserverCommand;
import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RelativeCommand;
import dynamake.commands.RemoveObserverCommand;
import dynamake.menubuilders.ActionRunner;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.CanvasModel;
import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocation;
import dynamake.models.Primitive;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.CreationBoundsFactory;
import dynamake.models.factories.PrimitiveSingletonFactory;
import dynamake.numbers.RectangleF;
import dynamake.tools.InteractionPresenter;
import dynamake.tools.TargetPresenter;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.NewChangeTransactionHandler;
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
						collector.commitTransaction();
					}
				});
			}
		};

		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionTargetContentMapBuilder.addMenuBuilder("Unforward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					Model.executeRemoveObserverFromObservable(collector, selection, target);
				}
			});
		} else {
			transactionTargetContentMapBuilder.addMenuBuilder("Forward to", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					Model.executeAddObserverFromObservable(collector, selection, target);
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
					ModelComponent referenceMC = ModelComponent.Util.closestCommonAncestor(selection, target);
					
					Location observableLocation = new ModelRootLocation();
					Location canvasModelLocation = new CompositeLocation(
						ModelComponent.Util.locationToAncestor(referenceMC, selection), 
						ModelComponent.Util.locationFromAncestor(referenceMC, target)
					);
					
					CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
					
					Location addedPrimitiveLocation = new CompositeLocation(
						canvasModelLocation,
						canvasModel.getNextLocation()
					);
					
					ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
					
					// Add
					pendingCommands.add(new PendingCommandState<Model>(
						new RelativeCommand<Model>(canvasModelLocation, 
							new CanvasModel.AddModelCommand(new CreationBoundsFactory(new RectangleF(dropBoundsOnTarget), new PrimitiveSingletonFactory(primImpl, dropBoundsOnTarget)))
						), 
						new RelativeCommand.Factory<Model>(new CanvasModel.RemoveModelCommand.AfterAdd()),
						new RelativeCommand.Factory<Model>(new CanvasModel.RestoreModelCommand.AfterRemove())
					));
					
					// Bind
					pendingCommands.add(new PendingCommandState<Model>(
						new AddObserverCommand(observableLocation, addedPrimitiveLocation),
						new RemoveObserverCommand(observableLocation, addedPrimitiveLocation)
					));
					
					PendingCommandFactory.Util.executeSequence(collector, referenceMC.getModelBehind(), pendingCommands, NewChangeTransactionHandler.class);
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
				
				collector.rejectTransaction();
			}
		});
	}
}
