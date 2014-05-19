package dynamake;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class ViewTool implements Tool {
	@Override
	public String getName() {
		return "View";
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
			productionPanel.editPanelMouseAdapter.showPopupForSelectionView(productionPanel.selectionFrame, e.getPoint(), null);

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
				if(productionPanel.editPanelMouseAdapter.output != null) {
					PropogationContext propCtx = new PropogationContext();
					ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
					DualCommand<Model> dualCommand = new DualCommandPair<Model>(
						new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
						new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
					);
					productionPanel.livePanel.getTransactionFactory().executeOnRoot(propCtx, dualCommand);
				}
				
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true);
				productionPanel.livePanel.repaint();
			}
		}
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) {

	}
}
