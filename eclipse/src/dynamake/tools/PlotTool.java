package dynamake.tools;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.RewrapCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.commands.UnwrapCommandFromScope;
import dynamake.commands.WrapCommandFromScope;
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
							
							@SuppressWarnings("unchecked")
							Location<Model>[] modelLocations = new Location[componentsWithinBounds.size()];
							for(int i = 0; i < modelLocations.length; i++) {
								ModelComponent view = componentsWithinBounds.get(i);
								modelLocations[i] = target.getLocationOf(view.getModelBehind());
							}
							
//							PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//								new WrapCommand(new RectangleF(creationBoundsInSelection), modelLocations), 
//								new UnwrapCommand.AfterWrap(),
//								new RewrapCommand.AfterUnwrap()
//							));
							
							collector.execute(new TriStatePURCommand<Model>(
								new CommandSequence<Model>(Arrays.asList(
									collector.createProduceCommand(new RectangleF(creationBoundsInSelection)),
									collector.createProduceCommand(modelLocations),
									new ReversibleCommandPair<Model>(new WrapCommandFromScope(), new UnwrapCommandFromScope())
								)),
								new CommandSequence<Model>(
									// Some sort of destroy command of a wrapping canvas?
									new ReversibleCommandPair<Model>(new UnwrapCommandFromScope(), new RewrapCommandFromScope())
								),
								new ReversibleCommandPair<Model>(new RewrapCommandFromScope(), new UnwrapCommandFromScope())
							));
						}
					});
				} else {
					collector.execute(new Trigger<Model>() {
						@Override
						public void run(Collector<Model> collector) {
							ModelFactory factory = new CreationBoundsFactory(new RectangleF(creationBoundsInSelection), new CanvasModelFactory());
							
							/*
							When forwarding this change, how is the production of the add model command supposed to be forwarded?
							I.e., the production of an add model is the location of the added model. This production should be forwarded
							as a production instruction with the forwarded location
							
							E.g.:
							the sequence:
							[produce(factory), add => l]
							should, based on the output scope of the sequence be forwarded as something like:
							[produce(forwarded(l)), addAt => forwarded(l)]
							
							Thus, somehow, it should be possible to make a forwarder for a command sequence based on an output scope.
							*/
							
							collector.execute(new TriStatePURCommand<Model>(
								new CommandSequence<Model>(Arrays.asList(
									collector.createProduceCommand(factory),
									new ReversibleCommandPair<Model>(new CanvasModel.AddModelCommand(null), new CanvasModel.RemoveModelCommand(null))
								)),
								new CommandSequence<Model>(
									new ReversibleCommandPair<Model>(new CanvasModel.DestroyModelCommand(null), /*RegenerateCommand?*/ null),
									new ReversibleCommandPair<Model>(new CanvasModel.RemoveModelCommand(null), new CanvasModel.RestoreModelCommand(null, null))
								),
								new ReversibleCommandPair<Model>(new CanvasModel.RestoreModelCommand(null, null), new CanvasModel.RemoveModelCommand(null))
							));
							
//							collector.execute(collector.createProduceCommand(factory));
//							
//							collector.execute(new TriStatePURCommand<Model>(
//								new ReversibleCommandPair<Model>(new CanvasModel.AddModelCommand(null), new CanvasModel.RemoveModelCommand(null)),
//								new CommandSequence<Model>(Arrays.asList(
//									new ReversibleCommandPair<Model>(new CanvasModel.DestroyModelCommand(null), /*RegenerateCommand?*/ null),
//									new ReversibleCommandPair<Model>(new CanvasModel.RemoveModelCommand(null), new CanvasModel.RestoreModelCommand(null, null))
//								)),
//								new ReversibleCommandPair<Model>(new CanvasModel.RestoreModelCommand(null, null), new CanvasModel.RemoveModelCommand(null))
//							));
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
