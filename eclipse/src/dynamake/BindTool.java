package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;

public class BindTool implements Tool {
	@Override
	public String getName() {
		return "Bind";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
				if(productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					targetModelComponent.getTransactionFactory().executeOnRoot(
						new PropogationContext(), new Model.RemoveObserver(productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()));
				} else {
					targetModelComponent.getTransactionFactory().executeOnRoot(
						new PropogationContext(), new Model.AddObserver(productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()));
				}
			}

			productionPanel.editPanelMouseAdapter.resetEffectFrame();

			if(productionPanel.targetFrame != null)
				productionPanel.remove(productionPanel.targetFrame);
			
			productionPanel.editPanelMouseAdapter.targetOver = null;
			productionPanel.livePanel.repaint();
		}
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			if(targetModelComponent != null) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true);
				productionPanel.livePanel.repaint();
			}
		}
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null && productionPanel.editPanelMouseAdapter.effectFrameMoving) {
			Point mouseOverPoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent newTargetOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
			ModelComponent newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(newTargetOver);
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null)
					productionPanel.remove(productionPanel.targetFrame);
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					Color color = 
						productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
						: ProductionPanel.BIND_COLOR;

					productionPanel.targetFrame.setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(Color.BLACK, 1), 
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(color, 3), 
								BorderFactory.createLineBorder(Color.BLACK, 1)
							)
						)
					);
					
					Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
						((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), productionPanel);
					productionPanel.targetFrame.setBounds(targetFrameBounds);
					productionPanel.targetFrame.setBackground(new Color(0, 0, 0, 0));
					productionPanel.add(productionPanel.targetFrame);
				}
			}
			
			int width = productionPanel.effectFrame.getWidth();
			int height = productionPanel.effectFrame.getHeight();

			Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			
			int x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
			int y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;

			productionPanel.effectFrame.setBounds(new Rectangle(x, y, width, height));
			productionPanel.livePanel.repaint();
		}
	}
}
