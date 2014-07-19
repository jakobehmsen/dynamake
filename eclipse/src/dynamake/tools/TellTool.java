package dynamake.tools;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public class TellTool implements Tool {
	@Override
	public String getName() {
		return "Tell";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		interactionPresenter.showPopupForSelectionTell(productionPanel, e.getPoint(), null, connection, interactionPresenter);
		
		collector.commit();
	}
	
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
		}
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		interactionPresenter.reset(collector);
	}
}
