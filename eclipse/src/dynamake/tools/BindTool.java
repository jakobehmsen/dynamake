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

public class BindTool implements Tool {
	@Override
	public String getName() {
		return "Bind";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final ModelComponent targetModelComponent = modelOver;
		
		final TranscriberBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		branch.close();
		
		targetPresenter.reset(branchStep2);
		targetPresenter = null;

		final ModelComponent selection = interactionPresenter.getSelection();
		interactionPresenter.reset(branchStep2);
		interactionPresenter = null;
		
		if(targetModelComponent != null && selection != targetModelComponent) {
			if(selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
				PropogationContext propCtx = new PropogationContext();
				branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
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
				PropogationContext propCtx = new PropogationContext();
				branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
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
			
			branchStep2.close();
		} else {
			branchStep2.reject();
		}
		
		mouseDown = null;
	}
	
	private Point mouseDown;
	private TranscriberBranch<Model> branch;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
		
		final TranscriberBranch<Model> branchStep1 = branch.branch();
		
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		ModelComponent targetModelComponent = modelOver;

		Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
		interactionPresenter = new InteractionPresenter(productionPanel);
		interactionPresenter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		
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
					return target != productionPanel.editPanelMouseAdapter.selection;
				}
			}
		);
		
		targetPresenter.update(modelOver, branchStep1);
		
		mouseDown = e.getPoint();
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			final RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			targetPresenter.update(modelOver, runBuilder);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = interactionPresenter.getSelectionFrameLocation().x + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = interactionPresenter.getSelectionFrameLocation().y + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			interactionPresenter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), runBuilder);
			
			runBuilder.execute();
		}
	}

	@Override
	public void paint(Graphics g) {

	}
}
