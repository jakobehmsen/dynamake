package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class DragTool implements Tool {
	@Override
	public String getName() {
		return "Drag";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		ModelComponent targetModelComponent = modelOver;
		
		final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		targetPresenter.reset(branchStep2);
		targetPresenter = null;
		
		if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
			productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
		} else {
			productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel, e.getPoint(), null, branchStep2);
		}
		
		branch.close();

		mouseDown = null;
	}
	
	private Point mouseDown;
	private PrevaylerServiceBranch<Model> branch;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		if(productionPanel.editPanelMouseAdapter.output != null) {
			PropogationContext propCtx = new PropogationContext();

			branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
					
					dualCommands.add(
						new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						)
					);
				}
			});
		}
		
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
		
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		}
		
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
		
		targetPresenter.update(modelOver, branchStep1);
		
		branchStep1.close();
		
		mouseDown = e.getPoint();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
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
}
