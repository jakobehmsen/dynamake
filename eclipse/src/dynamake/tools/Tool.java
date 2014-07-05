package dynamake.tools;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import dynamake.models.LiveModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.TranscriberCollector;

public interface Tool {
	String getName();

	void mouseMoved(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector);

	void mouseExited(LiveModel.ProductionPanel productionPanel, MouseEvent e, TranscriberCollector<Model> collector);

	void mouseReleased(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector);

	void mousePressed(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector);

	void mouseDragged(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector);

	void paint(Graphics g);

	void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector);
}
