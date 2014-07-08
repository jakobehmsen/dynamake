package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;

public abstract class RepetitiveCanvasTaskTool implements Tool {
	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		canvas = null;

//		final TranscriberBranch<Model> branchReset = branch.branch();
//		branchReset.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		targetPresenter.reset(collector);
		targetPresenter = null;
		
//		branchReset.close();
		
//		branch.close();
//		branch = null;
		
		collector.enlistCommit();
		collector.flush();
	}
	
//	private TranscriberBranch<Model> branch;
	private ModelComponent canvas;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
//		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
		
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
					return target == canvas;
				}
			}
		);
		
//		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		targetPresenter.update(canvas, collector);
		
//		runBuilder.execute();
		collector.flush();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection) {
		if(modelOver != canvas) {
			final ModelComponent modelOverParent = ModelComponent.Util.getParent(modelOver);
			
			if(modelOverParent == canvas) {
//				final TranscriberBranch<Model> branchSingleTask = branch.branch();
//				branchSingleTask.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				
				collector.execute(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						createDualCommandsForSingleTask(productionPanel, dualCommands, modelOverParent, modelOver);
					}
				});
				
//				branchSingleTask.close();
				collector.flush();
			}
		}
	}

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
//		final TranscriberBranch<Model> branchStep2 = branch.branch();
//		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		targetPresenter.reset(collector);
		targetPresenter = null;
		
//		branchStep2.close();
//		
//		branch.reject();
	}
	
	protected abstract void createDualCommandsForSingleTask(ProductionPanel productionPanel, List<DualCommand<Model>> dualCommands, ModelComponent canvas, ModelComponent modelOver);
}
