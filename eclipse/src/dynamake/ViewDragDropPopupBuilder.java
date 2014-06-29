package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.LivePanel;
import dynamake.transcription.TranscriberBranch;

public class ViewDragDropPopupBuilder implements DragDropPopupBuilder {
	private TranscriberBranch<Model> branch;
	
	public ViewDragDropPopupBuilder(TranscriberBranch<Model> branch) {
		this.branch = branch;
	}

	@Override
	public void buildFromSelectionAndTarget(final ModelComponent livePanel,
			JPopupMenu popup, final ModelComponent selection,
			final ModelComponent target, final Point dropPointOnTarget, final Rectangle dropBoundsOnTarget) {
		Runner runner = new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();

				((LivePanel)livePanel).productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branch);
				branch.close();
			}
		};
		
		CompositeMenuBuilder transactionTargetContentMapBuilder = new CompositeMenuBuilder();
		
		transactionTargetContentMapBuilder.addMenuBuilder("Appliance", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_APPLIANCE),
							new Model.SetPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));
						
						ModelComponent container = ModelComponent.Util.getParent(selection);
						if(container.getModelBehind().conformsToView(Model.VIEW_APPLIANCE))
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, selection.getTransactionFactory().getModelLocation())); // Absolute location
					}
				});
			}
		});
		
		transactionTargetContentMapBuilder.addMenuBuilder("Engineering", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Integer currentView = (Integer)selection.getModelBehind().getProperty(Model.PROPERTY_VIEW);
						if(currentView == null)
							currentView = Model.VIEW_APPLIANCE;
						
						// If the model is going to be hidden after the change, clear the current selection
						ModelComponent container = ModelComponent.Util.getParent(selection);
						if(!container.getModelBehind().conformsToView(Model.VIEW_ENGINEERING)) {
							((LivePanel)livePanel).productionPanel.editPanelMouseAdapter.createSelectCommands(null, dualCommands);
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, null)); // Absolute location
						}
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, Model.VIEW_ENGINEERING),
							new Model.SetPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_VIEW, currentView)
						));

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
		livePanel.productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branch);
		branch.reject();
	}
}
