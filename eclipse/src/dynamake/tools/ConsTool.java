package dynamake.tools;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public class ConsTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		final ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && interactionPresenter.getSelection() != targetModelComponent) {
			
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				interactionPresenter.showPopupForSelectionCons(productionPanel, mousePoint, targetModelComponent, connection, targetPresenter, interactionPresenter);
			} else {
				targetPresenter.reset(collector);
				
				if(interactionPresenter.getSelection().getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					final ModelComponent selection = interactionPresenter.getSelection();
					Model.executeRemoveObserver(collector, selection, targetModelComponent);
				} else {
					final ModelComponent selection = interactionPresenter.getSelection();
					Model.executeAddObserver(collector, selection, targetModelComponent);
				}
				
				interactionPresenter.reset(collector);

				collector.commit();
			}
		} else {
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				interactionPresenter.showPopupForSelectionCons(productionPanel, mousePoint, targetModelComponent, connection, targetPresenter, interactionPresenter);
			} else {
				targetPresenter.reset(collector);
				interactionPresenter.reset(collector);
				collector.reject();
				
				collector.afterNextTrigger(new Runnable() {
					@Override
					public void run() {
						productionPanel.livePanel.repaint();
					}
				});
			}
		}

		mouseDown = null;
	}
	
	private Point mouseDown;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)targetModelComponent);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromDefault(targetModelComponent, referencePoint, collector);
		}
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					if(target.getModelBehind() instanceof CanvasModel) {
						return ProductionPanel.TARGET_OVER_COLOR;
					} else {
						return interactionPresenter.getSelection().getModelBehind().isObservedBy(target.getModelBehind()) 
							? ProductionPanel.UNBIND_COLOR
							: ProductionPanel.BIND_COLOR;
					}
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
			
			final int x = cursorLocationInProductionPanel.x - width / 2;
			final int y = cursorLocationInProductionPanel.y - height / 2;
			
			interactionPresenter.changeEffectFrameDirect(new Rectangle(x, y, width, height), collector);
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
		interactionPresenter.reset(collector);
	}
}
