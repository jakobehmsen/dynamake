package dynamake.tools;

import java.awt.Point;

import javax.swing.JComponent;

import dynamake.models.LiveModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

/**
 * Instances of implementors represent dynamically applicable logic with a relatively fine grained focus.
 * Tools are short-lived entities only alive while they are being applied, where such an application is expected to be, typically, short and focused.
 * A tool is supposed to be performed on one or models doing its well-defined job.
 * A tool is further supposed to be applicable in general terms rather special terms.
 */
public interface Tool {
	void mouseReleased(LiveModel.ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint);
	void mousePressed(LiveModel.ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint);
	void mouseDragged(LiveModel.ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint);
	void rollback(ProductionPanel productionPanel, Collector<Model> collector);
}
