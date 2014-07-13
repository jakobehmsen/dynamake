package dynamake.tools;

import java.awt.event.MouseEvent;

import dynamake.models.LiveModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public interface Tool {
	String getName();
	void mouseMoved(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector);
	void mouseExited(LiveModel.ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector);
	void mouseReleased(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector);
	void mousePressed(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector);
	void mouseDragged(LiveModel.ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection);
	void rollback(ProductionPanel productionPanel, Collector<Model> collector);
}
