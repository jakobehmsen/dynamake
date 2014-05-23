package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.LiveModel.LivePanel;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();
				
				PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
				livePanel.getTransactionFactory().commitTransaction(propCtx);
			}
		};
		
		TransactionMapBuilder transactionTargetContentMapBuilder = new TransactionMapBuilder();
		
		transactionTargetContentMapBuilder.addTransaction("Appliance", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyOnRootTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_APPLIANCE),
							new Model.SetPropertyOnRootTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));
						
						ModelComponent container = ModelComponent.Util.getParent(selection);
						if(container.getModelBehind().conformsToView(Model.VIEW_APPLIANCE))
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, selection.getTransactionFactory().getModelLocation())); // Absolute location
					}
				});
			}
		});
		
		transactionTargetContentMapBuilder.addTransaction("Engineering", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyOnRootTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_ENGINEERING),
							new Model.SetPropertyOnRootTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));

						ModelComponent container = ModelComponent.Util.getParent(selection);
						if(container.getModelBehind().conformsToView(Model.VIEW_ENGINEERING))
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, selection.getTransactionFactory().getModelLocation())); // Absolute location
					}
				});
			}
		});
		
		transactionTargetContentMapBuilder.appendTo(popup, runner, "Selection to target");
	}
	
	@Override
	public void cancelPopup(LivePanel livePanel) {

	}
}
