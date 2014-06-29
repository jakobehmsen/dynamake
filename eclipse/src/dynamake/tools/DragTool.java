package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.RepaintRunBuilder;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.TranscriberBranch;

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
		System.out.println("Drag tool mouseReleased");
		
		ModelComponent targetModelComponent = modelOver;
		
		final TranscriberBranch<Model> branchStep2 = branch.branch();
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
	private TranscriberBranch<Model> branch;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		System.out.println("Drag tool mousePressed");
		
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		TranscriberBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = ModelComponent.Util.closestModelComponent(target);
		
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
		System.out.println("Drag tool mouseDragged (mouseDown=" + mouseDown + ")");
		
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

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}
