package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	private PrevaylerServiceBranch<Model> branch;
	
	public ConsDragDropPopupBuilder(PrevaylerServiceBranch<Model> branch) {
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
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addTransaction("Unforward to", new Runnable() {
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
				transactionTargetContentMapBuilder.addTransaction("Forward to", new Runnable() {
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
			
			TransactionMapBuilder transactionObserverContentMapBuilder = new TransactionMapBuilder();
			for(int i = 0; i < Primitive.getImplementationSingletons().length; i++) {
				final Primitive.Implementation primImpl = Primitive.getImplementationSingletons()[i];
				transactionObserverContentMapBuilder.addTransaction(primImpl.getName(), new Runnable() {
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
