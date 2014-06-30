package dynamake.tools;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.UnwrapTransaction;
import dynamake.commands.WrapTransaction;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
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
			JPopupMenu factoryPopopMenu = new JPopupMenu();
			
			final Rectangle creationBoundsInProductionPanel = productionPanel.editPanelMouseAdapter.getPlotBounds(mouseDown, e.getPoint());
			final Rectangle creationBoundsInSelection = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, productionPanel.selectionFrame);
			
			// Find components within the creation bounds of the selection
			final ArrayList<ModelComponent> componentsWithinBounds = new ArrayList<ModelComponent>();
			for(Component c: ((JComponent)productionPanel.editPanelMouseAdapter.selection).getComponents()) {
				if(creationBoundsInSelection.contains(c.getBounds())) {
					// Add in reverse order because views are positioned in the reverse order in the CanvasModel
					// This way, the views are sorted ascending index-wise
					componentsWithinBounds.add(0, (ModelComponent)c);
				}
			}
			
			final TranscriberBranch<Model> branchStep2 = branch.branch();
			
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			if(componentsWithinBounds.size() > 0) {
				JMenuItem factoryMenuItem = new JMenuItem();
				factoryMenuItem.setText("Wrap");
				
				factoryMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// Find the selected model and attempt an add model transaction
						// HACK: Models can only be added to canvases
						if(productionPanel.editPanelMouseAdapter.selection.getModelBehind() instanceof CanvasModel) {
							PropogationContext propCtx = new PropogationContext();
							
							branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
								@Override
								public void createDualCommands(List<DualCommand<Model>> dualCommands) {
									dualCommands.add(LiveModel.SetOutput.createDualBackward(productionPanel.livePanel));
									
									CanvasModel target = (CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind();
									Location targetLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
									int indexOfWrapper = target.getModelCount() - componentsWithinBounds.size();
									ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
									ModelLocation wrapperLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().extendLocation(wrapperLocationInTarget);
									
									// Each of the model locations should be moved from target to wrapper
									Location[] modelLocations = new Location[componentsWithinBounds.size()];
									int[] modelIndexes = new int[componentsWithinBounds.size()];
									for(int i = 0; i < modelLocations.length; i++) {
										ModelComponent view = componentsWithinBounds.get(i);
										modelLocations[i] = view.getTransactionFactory().getModelLocation();
										modelIndexes[i] = target.indexOfModel(view.getModelBehind());
									}
									
									dualCommands.add(new DualCommandPair<Model>(
										new WrapTransaction(targetLocation, creationBoundsInSelection, modelLocations), 
										new UnwrapTransaction(targetLocation, wrapperLocationInTarget, modelIndexes, creationBoundsInSelection)
									));
									
									dualCommands.add(LiveModel.SetOutput.createDualForward(productionPanel.livePanel, wrapperLocation));
								}
							});
							
							productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
							
							branchStep2.close();
							branch.close();
						}
					}
				});
				
				factoryPopopMenu.add(factoryMenuItem);
			}
			
			for(final Factory factory: productionPanel.livePanel.getFactories()) {
				JMenuItem factoryMenuItem = new JMenuItem();
				factoryMenuItem.setText(factory.getName());
				
				factoryMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// Find the selected model and attempt an add model transaction
						// HACK: Models can only be added to canvases
						if(productionPanel.editPanelMouseAdapter.selection.getModelBehind() instanceof CanvasModel) {
							PropogationContext propCtx = new PropogationContext();
							
							branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
								@Override
								public void createDualCommands(List<DualCommand<Model>> dualCommands) {
									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
									
									CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
									Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
									int index = canvasModel.getModelCount();
									Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
									// The location for Output depends on the side effect of add
									
									dualCommands.add(new DualCommandPair<Model>(
										new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInSelection, factory), 
										new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
									));
									
									dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation));
								}
							});
							
							productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
							branchStep2.close();
							branch.close();
						}
					}
				});
				
				factoryPopopMenu.add(factoryMenuItem);
			}
			
			factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) { }
				
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
				
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
					branch.reject();
				}
			});
			
			Point selectionReleasePointInSelection = SwingUtilities.convertPoint(((JComponent)(e.getSource())), e.getPoint(), productionPanel);
			factoryPopopMenu.show(productionPanel, selectionReleasePointInSelection.x + 10, selectionReleasePointInSelection.y);
			
			mouseDown = null;
		}
	}
	
	private TranscriberBranch<Model> branch;
	private Point mouseDown;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		TranscriberBranch<Model> branchStep1 = branch.branch();
		
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			mouseDown = e.getPoint();
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)modelOver);
			productionPanel.editPanelMouseAdapter.selectFromEmpty(modelOver, referencePoint, branchStep1);
		}
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			final Rectangle plotBoundsInProductionPanel = productionPanel.editPanelMouseAdapter.getPlotBounds(mouseDown, e.getPoint());
			
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(plotBoundsInProductionPanel, runBuilder);
			runBuilder.execute();
		}
	}

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}
