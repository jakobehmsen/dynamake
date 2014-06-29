package dynamake.tools;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import dynamake.models.LiveModel;
import dynamake.models.ModelComponent;

public interface Tool {
	String getName();

	void mouseMoved(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver);

	void mouseExited(LiveModel.ProductionPanel productionPanel, MouseEvent e);

	void mouseReleased(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver);

	void mousePressed(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver);

	void mouseDragged(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver);

	void paint(Graphics g);
}
