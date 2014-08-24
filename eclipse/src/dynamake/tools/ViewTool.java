package dynamake.tools;

import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public class ViewTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		interactionPresenter.showPopupForSelectionView(productionPanel, mousePoint, null, connection, interactionPresenter);
	}
	
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		ModelComponent targetModelComponent = modelOver;
		
		collector.startTransaction(targetModelComponent.getModelBehind(), NewChangeTransactionHandler.class);

		Point referencePoint = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)targetModelComponent);
		interactionPresenter = new InteractionPresenter(productionPanel);
		interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		interactionPresenter.reset(collector);
		collector.rejectTransaction();
	}
}
