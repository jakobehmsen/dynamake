package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.DualCommandFactory;
import dynamake.RepaintRunBuilder;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.LiveModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
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

		productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
		
		if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
			if(productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
				PropogationContext propCtx = new PropogationContext();
				branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
						Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
							new Model.AddObserver(observableLocation, observerLocation) // Absolute location
						));
						
						dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
					}
				});
			} else {
				PropogationContext propCtx = new PropogationContext();
				branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
						Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
						
						dualCommands.add(new DualCommandPair<Model>(
							new Model.AddObserver(observableLocation, observerLocation), // Absolute location
							new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
						));
						
						dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
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

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		final TranscriberBranch<Model> branchStep1 = branch.branch();
		
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		}
		
		// TODO: Consider whether to do the following:
		// Clear the output here on branchStep1
		// The same for all other tools
		// Create code to be reused across all tools for this
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(target.getModelBehind()) 
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
			
			final int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			final int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = productionPanel.selectionFrame.getX() + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = productionPanel.selectionFrame.getY() + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), runBuilder);
			
			runBuilder.execute();
		}
	}

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}
