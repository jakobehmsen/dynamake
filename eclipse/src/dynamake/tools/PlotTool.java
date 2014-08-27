package dynamake.tools;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandState;
import dynamake.commands.ConsumeCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ProduceCommand;
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
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.Trigger;

public class PlotTool implements Tool {
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		if(mouseDown != null) {
			final Rectangle creationBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, mousePoint);
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
							
							PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
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
//							ModelFactory factory = new CreationBoundsFactory(new RectangleF(creationBoundsInSelection), new CanvasModelFactory());
//							
//							PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//								new CanvasModel.AddModelCommand(factory),
//								new CanvasModel.RemoveModelCommand.AfterAdd(),
//								new CanvasModel.RestoreModelCommand.AfterRemove()
//							));
							
							ModelFactory factory = new CreationBoundsFactory(new RectangleF(creationBoundsInSelection), new CanvasModelFactory());
							
							ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
							
							pendingCommands.add(new PendingCommandState<Model>(
								new ProduceCommand<Model>(factory),
								new ConsumeCommand<Model>()
							));
							pendingCommands.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(null),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
							
							PendingCommandFactory.Util.executeSequence(collector, pendingCommands);
						}
					});
				}
			}

			interactionPresenter.reset(collector);
			
			collector.commitTransaction();
		}
	}
	
	private Point mouseDown;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			collector.startTransaction(modelOver.getModelBehind(), NewChangeTransactionHandler.class);
			
			mouseDown = mousePoint;
			Point referencePoint = SwingUtilities.convertPoint(sourceComponent, mousePoint, (JComponent)modelOver);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromEmpty(modelOver, referencePoint, collector);
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) {
		if(mouseDown != null) {
			final Rectangle plotBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, mousePoint);
			
			interactionPresenter.changeEffectFrameDirect(plotBoundsInProductionPanel, collector);
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		if(mouseDown != null) {
			interactionPresenter.reset(collector);
			collector.rejectTransaction();
		}
	}
}
