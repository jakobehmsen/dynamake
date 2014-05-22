package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class ConsDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();

				PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
				livePanel.getTransactionFactory().commitTransaction(propCtx);
			}
		};
		
		DualCommandFactory<Model> implicitDropAction = selection.getImplicitDropAction(target);
		
		if(implicitDropAction != null) {
			selection.getTransactionFactory().executeOnRoot(new PropogationContext(), implicitDropAction);

			PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
			livePanel.getTransactionFactory().commitTransaction(propCtx);
		} else {
			TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
			
			if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
				transactionTargetContentMapBuilder.addTransaction("Unforward to", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();
						
						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public DualCommand<Model> createDualCommand() {
								return new DualCommandPair<Model>(
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
								);
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
							}
						});
						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public dynamake.DualCommand<Model> createDualCommand() {
								return LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation());
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
							}
						});
					}
				});
			} else {
				transactionTargetContentMapBuilder.addTransaction("Forward to", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();

						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public DualCommand<Model> createDualCommand() {
								return new DualCommandPair<Model>(
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
								);
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
							}
						});
						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public dynamake.DualCommand<Model> createDualCommand() {
								return LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation());
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
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
						target.getTransactionFactory().executeOnRoot(new PropogationContext(), new AddThenBindAndOutputTransaction(
							livePanel.getTransactionFactory().getModelLocation(),
							selection.getTransactionFactory().getModelLocation(), 
							target.getTransactionFactory().getModelLocation(), 
							new PrimitiveSingletonFactory(primImpl), 
							dropBoundsOnTarget
						));
						
						if(1 != 2) {
							return;
						}
						
						PropogationContext propCtx = new PropogationContext();
						
						target.getTransactionFactory().executeOnRoot(new PropogationContext(), 
							new CanvasModel.AddModelTransaction(target.getTransactionFactory().getModelLocation(), dropBoundsOnTarget, new PrimitiveSingletonFactory(primImpl)));
						
						target.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
//							@Override
//							public DualCommand createDualCommand() {
//								// Add
//								// Bind
//								// Output
//								// The location for Bind and Output depends on the side effect of add
//								
//								// TODO Auto-generated method stub
//								return null;
//							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								// Add
								// Bind
								// Output
								// The location for Bind and Output depends on the side effect of add
							}
						});
						
//						AddThenBindAndOutputTransaction(
//							livePanel.getTransactionFactory().getModelLocation(),
//							selection.getTransactionFactory().getModelLocation(), 
//							target.getTransactionFactory().getModelLocation(), 
//							new PrimitiveSingletonFactory(primImpl), 
//							dropBoundsOnTarget
//						));
						
						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public DualCommand<Model> createDualCommand() {
								return new DualCommandPair<Model>(
									new Model.AddObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
									new Model.RemoveObserver(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
								);
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
							}
						});

						selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
							public dynamake.DualCommand<Model> createDualCommand() {
								return LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation());
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
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
		PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_ROLLBACK);
		livePanel.getTransactionFactory().rollbackTransaction(propCtx);
	}
}
