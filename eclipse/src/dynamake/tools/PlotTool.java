package dynamake.tools;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RewrapCommand;
import dynamake.commands.UnwrapCommand;
import dynamake.commands.WrapCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.CanvasModelFactory;
import dynamake.models.factories.CreationBoundsFactory;
import dynamake.models.factories.ModelFactory;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.ExPendingCommandFactory2;
import dynamake.transcription.LocalHistoryHandler;
import dynamake.transcription.Trigger;

public class PlotTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		if(mouseDown != null) {
			final Rectangle creationBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, e.getPoint());
			final Rectangle creationBoundsInSelection = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, interactionPresenter.getSelectionFrame());
			
			// Find components within the creation bounds of the selection
			final ArrayList<ModelComponent> componentsWithinBounds = new ArrayList<ModelComponent>();
			for(Component c: ((JComponent)interactionPresenter.getSelection()).getComponents()) {
				if(creationBoundsInSelection.contains(c.getBounds())) {
					// Add in reverse order because views are positioned in the reverse order in the CanvasModel
					// This way, the views are sorted ascending index-wise
					componentsWithinBounds.add(0, (ModelComponent)c);
				}
			}

			if(interactionPresenter.getSelection().getModelBehind() instanceof CanvasModel) {
				final ModelComponent selection = interactionPresenter.getSelection();
				// Wrap if one more models are contained within the effect frame
				if(componentsWithinBounds.size() > 0) {
					collector.execute(new Trigger<Model>() {
						@Override
						public void run(Collector<Model> collector) {
							CanvasModel target = (CanvasModel)selection.getModelBehind();
							
							Location[] modelLocations = new Location[componentsWithinBounds.size()];
							for(int i = 0; i < modelLocations.length; i++) {
								ModelComponent view = componentsWithinBounds.get(i);
								modelLocations[i] = target.getLocationOf(view.getModelBehind());
							}
							
							ExPendingCommandFactory2.Util.single(collector, selection.getModelBehind(), LocalHistoryHandler.class, new PendingCommandState<Model>(
								new WrapCommand(new RectangleF(creationBoundsInSelection), modelLocations), 
								new UnwrapCommand.AfterWrap(),
								new RewrapCommand.AfterUnwrap()
							));
						}
					});
				} else {
					collector.execute(new Trigger<Model>() {
						@Override
						public void run(Collector<Model> collector) {
							ModelFactory factory = new CreationBoundsFactory(new RectangleF(creationBoundsInSelection), new CanvasModelFactory());
							
							ExPendingCommandFactory2.Util.single(collector, selection.getModelBehind(), LocalHistoryHandler.class, new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(factory),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
						}
					});
				}
			}

			interactionPresenter.reset(collector);
			
			collector.commit();
		}
	}
	
	private Point mouseDown;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			mouseDown = e.getPoint();
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)modelOver);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromEmpty(modelOver, referencePoint, collector);
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		if(mouseDown != null) {
			final Rectangle plotBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, e.getPoint());
			
			interactionPresenter.changeEffectFrameDirect(plotBoundsInProductionPanel, collector);
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		if(mouseDown != null) {
			interactionPresenter.reset(collector);
		}
	}
}
