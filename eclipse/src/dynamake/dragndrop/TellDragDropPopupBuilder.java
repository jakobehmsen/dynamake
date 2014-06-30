package dynamake.dragndrop;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPopupMenu;

import dynamake.Runner;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.TellPropertyTransaction;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.LivePanel;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberBranch;

public class TellDragDropPopupBuilder implements DragDropPopupBuilder {
	private TranscriberBranch<Model> branch;
	
	public TellDragDropPopupBuilder(TranscriberBranch<Model> branch) {
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
		
		transactionTargetContentMapBuilder.addMenuBuilder("Tell Color", new Runnable() {
			@Override
			public void run() {
				PropogationContext propCtx = new PropogationContext();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						dualCommands.add(new DualCommandPair<Model>(
							new TellPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_COLOR),
							new TellPropertyTransaction(selection.getTransactionFactory().getModelLocation(), Model.PROPERTY_COLOR)
						));
						
						dualCommands.add(LiveModel.SetOutput.createDual((LivePanel)livePanel, selection.getTransactionFactory().getModelLocation()));
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
