package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public class DragTool implements Tool {
	@Override
	public String getName() {
		return "Drag";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && interactionPresenter.getSelection() != targetModelComponent) {
			interactionPresenter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, connection, targetPresenter, interactionPresenter);
		} else {
			interactionPresenter.showPopupForSelectionObject(productionPanel, e.getPoint(), targetModelComponent, connection, targetPresenter, interactionPresenter);
		}

		targetPresenter = null;
		interactionPresenter = null;

		mouseDown = null;
	}
	
	private Point mouseDown;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = ModelComponent.Util.closestModelComponent(target);
		
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
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
		
		mouseDown = e.getPoint();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		if(mouseDown != null) {
			targetPresenter.update(modelOver, collector);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = interactionPresenter.getSelectionFrameLocation().x + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = interactionPresenter.getSelectionFrameLocation().y + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			interactionPresenter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), collector);
		}
	}

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
		interactionPresenter.reset(collector);
	}
}
