package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.DualCommand;
import dynamake.DualCommandFactory;
import dynamake.RepaintRunBuilder;
import dynamake.TargetPresenter;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.TranscriberBranch;

public class TrimTool implements Tool {
	@Override
	public String getName() {
		return "Trim";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		canvas = null;

		final TranscriberBranch<Model> branchReset = branch.branch();
		branchReset.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		targetPresenter.reset(branchReset);
		targetPresenter = null;
		
		branchReset.close();
		
		branch.close();
		branch = null;
	}
	
	private TranscriberBranch<Model> branch;
	private ModelComponent canvas;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		canvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return ProductionPanel.TARGET_OVER_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != productionPanel.editPanelMouseAdapter.selection;
				}
			}
		);
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		targetPresenter.update(canvas, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver) {
		if(modelOver != canvas) {
			ModelComponent modelOverParent = ModelComponent.Util.getParent(modelOver);
			
			if(modelOverParent == canvas) {
				final TranscriberBranch<Model> branchDelete = branch.branch();
				branchDelete.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				
				branchDelete.execute(new PropogationContext(), new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						CanvasModel.appendRemoveTransaction(dualCommands, productionPanel.livePanel, modelOver, canvas.getTransactionFactory().getModelLocation(), (CanvasModel)canvas.getModelBehind());
					}
				});
				
				branchDelete.close();
			}
		}
	}

	@Override
	public void paint(Graphics g) {

	}
}
