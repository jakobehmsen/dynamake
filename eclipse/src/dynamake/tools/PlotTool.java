package dynamake.tools;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.UnwrapToLocationsTransaction;
import dynamake.commands.WrapTransaction;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.CanvasModelFactory;
import dynamake.models.factories.Factory;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;

public class PlotTool implements Tool {
	@Override
	public String getName() {
		return "Plot";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
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
					collector.execute(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							CanvasModel target = (CanvasModel)selection.getModelBehind();
							Location targetLocation = selection.getModelTranscriber().getModelLocation();
							int indexOfWrapper = target.getModelCount() - componentsWithinBounds.size();
							ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
							
							// Each of the model locations should be moved from target to wrapper
							Location[] modelLocations = new Location[componentsWithinBounds.size()];
							int[] modelIndexes = new int[componentsWithinBounds.size()];
							for(int i = 0; i < modelLocations.length; i++) {
								ModelComponent view = componentsWithinBounds.get(i);
								modelLocations[i] = view.getModelTranscriber().getModelLocation();
								modelIndexes[i] = target.indexOfModel(view.getModelBehind());
							}
							
							dualCommands.add(new DualCommandPair<Model>(
								new WrapTransaction(targetLocation, creationBoundsInSelection, modelLocations), 
								new UnwrapToLocationsTransaction(targetLocation, wrapperLocationInTarget, modelIndexes, creationBoundsInSelection)
							));
						}
					});
				} else {
					final Factory factory = new CanvasModelFactory();
					
					collector.execute(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							ModelComponent target = selection;
							
							CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
							Location canvasModelLocation = target.getModelTranscriber().getModelLocation();
							int index = canvasModel.getModelCount();
							
							dualCommands.add(new DualCommandPair<Model>(
								new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInSelection, factory), 
								new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
							));
						}
					});
				}
			}

			interactionPresenter.reset(collector);
			interactionPresenter = null;
			
			collector.enlistCommit();
			collector.flush();
			
			mouseDown = null;
		}
	}
	
	private Point mouseDown;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			mouseDown = e.getPoint();
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)modelOver);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromEmpty(modelOver, referencePoint, collector);
		}
		
		collector.flush();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection) {
		if(mouseDown != null) {
			final Rectangle plotBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, e.getPoint());
			
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			interactionPresenter.changeEffectFrameDirect2(plotBoundsInProductionPanel, runBuilder);
			runBuilder.execute();
		}
	}

	@Override
	public void paint(Graphics g) {

	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		interactionPresenter.reset(collector);
		interactionPresenter = null;
	}
}
