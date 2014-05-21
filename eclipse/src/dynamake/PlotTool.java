package dynamake;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import dynamake.CanvasModel.IndexLocation;
import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;
import dynamake.LiveModel.ProductionPanel.EditPanelMouseAdapter;

public class PlotTool implements Tool {
	@Override
	public String getName() {
		return "Plot";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			JPopupMenu factoryPopopMenu = new JPopupMenu();
			
			Point selectionReleasePoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
			final Rectangle creationBounds = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionReleasePoint);
			
			final Rectangle selectionCreationBounds = SwingUtilities.convertRectangle(productionPanel, creationBounds, ((JComponent)(e.getSource())).getParent());
			
			// Find target model component
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			// Find components within the creation bounds
			final ArrayList<ModelComponent> componentsWithinBounds = new ArrayList<ModelComponent>();
			for(Component c: ((JComponent)targetModelComponent).getComponents()) {
				if(selectionCreationBounds.contains(c.getBounds())) {
//					componentsWithinBounds.add((ModelComponent)c);
					// Add in reverse order because views are positioned in the reverse order in the CanvasModel
					// This way, the views are sorted ascending index-wise
					componentsWithinBounds.add(0, (ModelComponent)c);
				}
			}
			
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

							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
								@Override
								public DualCommand<Model> createDualCommand() {
									Location[] modelLocations = new Location[componentsWithinBounds.size()];
									int[] modelIndexes = new int[componentsWithinBounds.size()];
									for(int i = 0; i < modelLocations.length; i++) {
										ModelComponent view = componentsWithinBounds.get(i);
										modelLocations[i] = view.getTransactionFactory().getModelLocation();
										modelIndexes[i] = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).indexOfModel(view.getModelBehind());
									}
									Location outputLocation = productionPanel.livePanel.model.getOutput() != null ? productionPanel.livePanel.model.getOutput().getLocator().locate() : null;
									int wrapperIndex = ((CanvasModel)productionPanel.editPanelMouseAdapter.selection.getModelBehind()).getModelCount() - modelLocations.length;

									return new DualCommandPair<Model>(
										new WrapTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(),
											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
											selectionCreationBounds, 
											modelLocations),
										new UnwrapTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
											new CanvasModel.IndexLocation(wrapperIndex), 
											modelIndexes,
											selectionCreationBounds,
											outputLocation)
									);
								}
							});
							
							PropogationContext commitPropCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().commitTransaction(commitPropCtx);

							productionPanel.editPanelMouseAdapter.resetEffectFrame();
							productionPanel.livePanel.repaint();
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
							
							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
								@Override
								public DualCommand<Model> createDualCommand() {
									ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
									int addIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
									ModelComponent output = productionPanel.editPanelMouseAdapter.output;
									Location outputLocation = null;
									if(output != null)
										outputLocation = output.getTransactionFactory().getModelLocation();
									
									return new DualCommandPair<Model>(
										new AddThenOutputTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
											productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
											creationBounds, 
											factory), 
										new SetOutputThenRemoveAtTransaction(
											productionPanel.livePanel.getTransactionFactory().getModelLocation(), 
											outputLocation, 
											target.getTransactionFactory().getModelLocation(), 
											addIndex
										)
									);
								}
							});
							
							PropogationContext commitPropCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().commitTransaction(commitPropCtx);

							productionPanel.editPanelMouseAdapter.resetEffectFrame();
							productionPanel.livePanel.repaint();
						}
					}
				});
				
				factoryPopopMenu.add(factoryMenuItem);
			}
			
			factoryPopopMenu.addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) { 
					productionPanel.livePanel.repaint();
				}
				
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
//						productionPanel.editPanelMouseAdapter.resetEffectFrame();
						productionPanel.livePanel.repaint();
					}
				}
				
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_ROLLBACK);
					productionPanel.livePanel.getTransactionFactory().rollbackTransaction(propCtx);
				}
			});
			
			Point selectionReleasePointInSelection = SwingUtilities.convertPoint(((JComponent)(e.getSource())), e.getPoint(), productionPanel);
			factoryPopopMenu.show(productionPanel, selectionReleasePointInSelection.x + 10, selectionReleasePointInSelection.y);
		}
	}

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e) {
		if(e.getButton() == 1) {
			productionPanel.livePanel.getTransactionFactory().beginTransaction();
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();
				
				productionPanel.livePanel.getTransactionFactory().executeOnRoot(propCtx, new DualCommandFactory<Model>() {
					@Override
					public DualCommand<Model> createDualCommand() {
						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
						return new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						);
					}
				});
			}

			Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			if(targetModelComponent != null && targetModelComponent.getModelBehind() instanceof CanvasModel) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromEmpty(targetModelComponent, referencePoint, true);
				productionPanel.livePanel.repaint();
			} else {
				productionPanel.editPanelMouseAdapter.selectionMouseDown = e.getPoint();
			}
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			Point selectionDragPoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
			Rectangle plotBoundsInSelection = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionDragPoint);
			final Rectangle plotBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)productionPanel.editPanelMouseAdapter.selection, plotBoundsInSelection, productionPanel);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.effectFrame.setBounds(plotBoundsInProductionPanel);
					productionPanel.livePanel.repaint();
				}
			});
//			Point selectionDragPoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
//			Rectangle plotBoundsInSelection = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionDragPoint);
//			Rectangle plotBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)productionPanel.editPanelMouseAdapter.selection, plotBoundsInSelection, productionPanel);
//			productionPanel.effectFrame.setBounds(plotBoundsInProductionPanel);
//			productionPanel.livePanel.repaint();
		}
	}
}
