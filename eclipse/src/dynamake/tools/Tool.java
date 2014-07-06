package dynamake.tools;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import dynamake.models.LiveModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;

public interface Tool {
	String getName();

	void mouseMoved(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector);

	void mouseExited(LiveModel.ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector);

	void mouseReleased(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector);

	void mousePressed(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector);

	void mouseDragged(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection);

	void paint(Graphics g);

	void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector);
}
