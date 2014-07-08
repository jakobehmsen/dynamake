package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberOnFlush;

public class ConsTool implements Tool {
	@Override
	public String getName() {
		return "Cons";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		final ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && interactionPresenter.getSelection() != targetModelComponent) {
			
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				interactionPresenter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, connection, targetPresenter, interactionPresenter);
				targetPresenter = null;
				interactionPresenter = null;
			} else {
				targetPresenter.reset(collector);
				targetPresenter = null;
				
				if(interactionPresenter.getSelection().getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					final ModelComponent selection = interactionPresenter.getSelection();

					collector.enlist(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getModelTranscriber().getModelLocation();
							Location observerLocation = targetModelComponent.getModelTranscriber().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
								new Model.AddObserver(observableLocation, observerLocation) // Absolute location
							));
						}
					});
				} else {
					final ModelComponent selection = interactionPresenter.getSelection();
					collector.enlist(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = selection.getModelTranscriber().getModelLocation();
							Location observerLocation = targetModelComponent.getModelTranscriber().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.AddObserver(observableLocation, observerLocation), // Absolute location
								new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
							));
						}
					});
				}
				
				interactionPresenter.reset(collector);
				interactionPresenter = null;

				collector.enlistCommit();
				collector.flush();
			}
		} else {
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				interactionPresenter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, connection, targetPresenter, interactionPresenter);
				targetPresenter = null;
				interactionPresenter = null;
			} else {
				targetPresenter.reset(collector);
				targetPresenter = null;
				interactionPresenter.reset(collector);
				interactionPresenter = null;
				collector.enlistReject();
				
				collector.afterNextTrigger(new Runnable() {
					@Override
					public void run() {
						productionPanel.livePanel.repaint();
					}
				});
				collector.flush();
			}
		}

		mouseDown = null;
	}
	
	private Point mouseDown;
	private TargetPresenter targetPresenter;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			
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
		
		mouseDown = e.getPoint();

		collector.flush();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection) {
		if(mouseDown != null) {
			targetPresenter.update(modelOver, collector);
			
			final int width = interactionPresenter.getEffectFrameWidth();
			final int height = interactionPresenter.getEffectFrameHeight();
			
			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = cursorLocationInProductionPanel.x - width / 2;
			final int y = cursorLocationInProductionPanel.y - height / 2;
			
			interactionPresenter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), collector);

			collector.flush();
		}
	}

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		targetPresenter.reset(collector);
		targetPresenter = null;

		interactionPresenter.reset(collector);
		interactionPresenter = null;
	}
}
