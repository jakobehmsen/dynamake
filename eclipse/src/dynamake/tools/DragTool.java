package dynamake.tools;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.NewChangeTransactionHandler;

public class DragTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && interactionPresenter.getSelection() != targetModelComponent) {
			interactionPresenter.showPopupForSelectionObject(productionPanel, mousePoint, targetModelComponent, connection, targetPresenter, interactionPresenter);
		} else {
			interactionPresenter.showPopupForSelectionObject(productionPanel, mousePoint, targetModelComponent, connection, targetPresenter, interactionPresenter);
		}
	}
	
	private Point mouseDown;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		Point pointInContentView = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = ModelComponent.Util.closestModelComponent(target);
		
		collector.startTransaction(targetModelComponent.getModelBehind(), NewChangeTransactionHandler.class);
		
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)targetModelComponent);
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromView(targetModelComponent, referencePoint, collector);
		}
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return ProductionPanel.TARGET_OVER_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target != interactionPresenter.getSelection();
				}
			}
		);
		
		targetPresenter.update(modelOver, collector);
		
		mouseDown = mousePoint;
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) {
		if(mouseDown != null) {
			targetPresenter.update(modelOver, collector);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = mousePoint;
			
			final int x = interactionPresenter.getSelectionFrameLocation().x + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = interactionPresenter.getSelectionFrameLocation().y + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			interactionPresenter.changeEffectFrameDirect(new Rectangle(x, y, width, height), collector);
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
		interactionPresenter.reset(collector);
		collector.rejectTransaction();
	}
}
