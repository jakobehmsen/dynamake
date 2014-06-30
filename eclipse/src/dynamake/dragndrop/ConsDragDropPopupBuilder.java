package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.Runner;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.Primitive;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.factories.PrimitiveSingletonFactory;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberBranch;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	private TranscriberBranch<Model> branch;
	
	public ConsDragDropPopupBuilder(TranscriberBranch<Model> branch) {
		this.branch = branch;
	}

	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();

				((LivePanel)livePanel).productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branch);
				branch.close();
			}
		};
		
		DualCommandFactory<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			branch.execute(new PropogationContext(), implicitDropAction);

			((LivePanel)livePanel).productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branch);
			branch.close();
		} else {
			CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addMenuBuilder("Unforward to", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();
						
						branch.execute(propCtx, new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
								));
								
								dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation()));
							}
						});
					}
				});
			} else {
				transactionTargetContentMapBuilder.addMenuBuilder("Forward to", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();

						branch.execute(propCtx, new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
								));
								
								dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation()));
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
				transactionObserverContentMapBuilder.addMenuBuilder(primImpl.getName(), new Runnable() {
					@Override
					public void run() {
						final PropogationContext propCtx = new PropogationContext();
						
						branch.execute(propCtx, new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
								Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
								int index = canvasModel.getModelCount();
								Location addedPrimitiveLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
								// The location for Bind and Output depends on the side effect of add
								
								// Add
								dualCommands.add(new DualCommandPair<Model>(
									new CanvasModel.AddModelTransaction(canvasModelLocation, dropBoundsOnTarget, new PrimitiveSingletonFactory(primImpl)), 
									new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
								));

								// Bind
								dualCommands.add(new DualCommandPair<Model>(
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), addedPrimitiveLocation), // Absolute location
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), addedPrimitiveLocation) // Absolute location
								));
								
								// Output
								dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, addedPrimitiveLocation)); // Absolute location
							}
						});
					}
				});
			}
			transactionObserverContentMapBuilder.appendTo(popup, runner, "Observation");
		}
	}

	@Override
	public void cancelPopup(LivePanel livePanel) {
		livePanel.productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branch);
		branch.reject();
	}
}
