package dynamake;

import java.awt.event.MouseEvent;

public interface Tool {
	String getName();

	void mouseMoved(LiveModel.ProductionPanel productionPanel, MouseEvent e);

	void mouseExited(LiveModel.ProductionPanel productionPanel, MouseEvent e);

	void mouseReleased(LiveModel.ProductionPanel productionPanel, MouseEvent e);

	void mousePressed(LiveModel.ProductionPanel productionPanel, MouseEvent e);

	void mouseDragged(LiveModel.ProductionPanel productionPanel, MouseEvent e);
}
