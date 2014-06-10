package dynamake;

import java.awt.event.MouseEvent;

import dynamake.LiveModel.ProductionPanel;

public class UndoTool implements Tool {
@Override
	public String getName() {
		return "Undo";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) { }

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		productionPanel.livePanel.undo();
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }
}
