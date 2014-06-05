package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class DragDragDropPopupBuilder implements DragDropPopupBuilder {
	private PrevaylerServiceBranch<Model> branch;
	
	public DragDragDropPopupBuilder(PrevaylerServiceBranch<Model> branch) {
		this.branch = branch;
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
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();
				
				branch.close();
			}
		};
		
		ModelComponent parentModelComponent = ModelComponent.Util.closestModelComponent(((JComponent)selection).getParent()); 
		
		TransactionMapBuilder containerTransactionMapBuilder = new TransactionMapBuilder();
		if(parentModelComponent != null)
			parentModelComponent.appendContainerTransactions(containerTransactionMapBuilder, selection, branch);

		TransactionMapBuilder transactionSelectionMapBuilder = new TransactionMapBuilder();
		selection.appendTransactions(livePanel, transactionSelectionMapBuilder, branch);

		containerTransactionMapBuilder.appendTo(popup, runner, "Container");
		if(!containerTransactionMapBuilder.isEmpty() && !transactionSelectionMapBuilder.isEmpty())
			popup.addSeparator();
		transactionSelectionMapBuilder.appendTo(popup, runner, "Selection");
	}

	private void buildFromSelectionToOther(final ModelComponent livePanel, JPopupMenu popup, final ModelComponent selection, final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();
				
				branch.close();
			}
		};
		
		TransactionMapBuilder transactionSelectionGeneralMapBuilder = new TransactionMapBuilder();
		
		if(selection.getModelBehind().isObservedBy(target.getModelBehind())) {
			transactionSelectionGeneralMapBuilder.addTransaction("Unforward to", new Runnable() {
				@Override
				public void run() {
					PropogationContext propCtx = new PropogationContext();
					branch.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getTransactionFactory().getModelLocation();
							Location observerLocation = target.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
								new Model.AddObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, observerLocation)); // Absolute location
						}
					});
				}
			});
		} else {
			transactionSelectionGeneralMapBuilder.addTransaction("Forward to", new Runnable() {
				@Override
				public void run() {
					PropogationContext propCtx = new PropogationContext();
					branch.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getTransactionFactory().getModelLocation();
							Location observerLocation = target.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.AddObserver(observableLocation, observerLocation), // Absolute location
								new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, observerLocation)); // Absolute location
						}
					});
				}
			});
		}
		
		transactionSelectionGeneralMapBuilder.addTransaction("Inject", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(new DualCommandPair<Model>(
							new InjectTransaction(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation()),
							new DejectTransaction(selection.getTransactionFactory().getModelLocation(), target.getTransactionFactory().getModelLocation())
						));

						dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, target.getTransactionFactory().getModelLocation())); // Absolute location
					}
				});
			}
		});

		TransactionMapBuilder transactionTargetMapBuilder = new TransactionMapBuilder();
		target.appendDropTargetTransactions(livePanel, selection, dropBoundsOnTarget, dropPointOnTarget, transactionTargetMapBuilder, branch);
		
		transactionSelectionGeneralMapBuilder.appendTo(popup, runner, "General");
		if(!transactionSelectionGeneralMapBuilder.isEmpty() && !transactionTargetMapBuilder.isEmpty())
			popup.addSeparator();
		transactionTargetMapBuilder.appendTo(popup, runner, "Target");

		TransactionMapBuilder transactionDroppedMapBuilder = new TransactionMapBuilder();
		selection.appendDroppedTransactions(livePanel, target, dropBoundsOnTarget, transactionDroppedMapBuilder, branch);
		if(!transactionTargetMapBuilder.isEmpty() && !transactionDroppedMapBuilder.isEmpty())
			popup.addSeparator();
		transactionDroppedMapBuilder.appendTo(popup, runner, "Dropped");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {
		branch.reject();
	}
}
