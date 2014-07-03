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
import dynamake.commands.UnwrapTransaction;
import dynamake.commands.WrapTransaction;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.CanvasModelFactory;
import dynamake.models.factories.Factory;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.TranscriberBranch;

public class PlotTool implements Tool {
	@Override
	public String getName() {
		return "Plot";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
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
			
			final TranscriberBranch<Model> branchStep2 = branch.branch();
			
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

			if(interactionPresenter.getSelection().getModelBehind() instanceof CanvasModel) {
				final ModelComponent selection = interactionPresenter.getSelection();
				// Wrap if one more models are contained within the effect frame
				if(componentsWithinBounds.size() > 0) {
					PropogationContext propCtx = new PropogationContext();
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
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
								new UnwrapTransaction(targetLocation, wrapperLocationInTarget, modelIndexes, creationBoundsInSelection)
							));
						}
					});
				} else {
					final Factory factory = new CanvasModelFactory();
					
					PropogationContext propCtx = new PropogationContext();
					
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
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

			interactionPresenter.reset(branchStep2);
			interactionPresenter = null;
			
			branchStep2.close();
			branch.close();
			
			mouseDown = null;
		}
	}
	
	private TranscriberBranch<Model> branch;
	private Point mouseDown;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getModelTranscriber().createBranch();
		
		TranscriberBranch<Model> branchStep1 = branch.branch();
		
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			mouseDown = e.getPoint();
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)modelOver);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromEmpty(modelOver, referencePoint, branchStep1);
		}
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver) {
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
	public void rollback(ProductionPanel productionPanel) {
		final TranscriberBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));

		interactionPresenter.reset(branchStep2);
		interactionPresenter = null;
		
		branchStep2.close();
		
		branch.reject();
	}
}
