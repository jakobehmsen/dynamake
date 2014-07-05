package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;

public class BindTool implements Tool {
	@Override
	public String getName() {
		return "Bind";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		final ModelComponent targetModelComponent = modelOver;
		
//		final TranscriberBranch<Model> branchStep2 = branch.branch();
//		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
//		
//		branch.close();
		
		targetPresenter.reset(collector);
		targetPresenter = null;

		final ModelComponent selection = interactionPresenter.getSelection();
		interactionPresenter.reset(collector);
		interactionPresenter = null;
		
		if(targetModelComponent != null && selection != targetModelComponent) {
			if(selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
//				PropogationContext propCtx = new PropogationContext();
				collector.enqueue(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Location observableLocation = selection.getModelTranscriber().getModelLocation();
						Location observerLocation = targetModelComponent.getModelTranscriber().getModelLocation();
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
							new Model.AddObserver(observableLocation, observerLocation) // Absolute location
						));
					}
				});
			} else {
//				PropogationContext propCtx = new PropogationContext();
				collector.enqueue(new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Location observableLocation = selection.getModelTranscriber().getModelLocation();
						Location observerLocation = targetModelComponent.getModelTranscriber().getModelLocation();
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.AddObserver(observableLocation, observerLocation), // Absolute location
							new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
						));
					}
				});
			}
			
//			branchStep2.close();
			collector.commit();
		} else {
			collector.reject();
//			branchStep2.reject();
		}
		
		mouseDown = null;
	}
	
	private Point mouseDown;
//	private TranscriberBranch<Model> branch;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
//		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
		
//		final TranscriberBranch<Model> branchStep1 = branch.branch();
		
//		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		ModelComponent targetModelComponent = modelOver;

		Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
		interactionPresenter = new InteractionPresenter(productionPanel);
		interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return interactionPresenter.getSelection().getModelBehind().isObservedBy(target.getModelBehind()) 
						? ProductionPanel.UNBIND_COLOR 
						: ProductionPanel.BIND_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != interactionPresenter.getSelection();
				}
			}
		);
		
		targetPresenter.update(modelOver, collector);
		
		mouseDown = e.getPoint();
		
//		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector) {
		if(mouseDown != null) {
//			final RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			targetPresenter.update(modelOver, collector);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = interactionPresenter.getSelectionFrameLocation().x + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = interactionPresenter.getSelectionFrameLocation().y + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			interactionPresenter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), collector);
			
//			runBuilder.execute();
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

		interactionPresenter.reset(collector);
		interactionPresenter = null;
		
//		branchStep2.close();
//		
//		branch.reject();
	}
}
