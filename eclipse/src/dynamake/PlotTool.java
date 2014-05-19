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

import dynamake.LiveModel.ProductionPanel;

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
					componentsWithinBounds.add((ModelComponent)c);
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
							Location[] modelLocation = new Location[componentsWithinBounds.size()];
							for(int i = 0; i < modelLocation.length; i++) {
								modelLocation[i] = componentsWithinBounds.get(i).getTransactionFactory().getModelLocation();
							}
							
							PropogationContext propCtx = new PropogationContext();
							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().executeOnRoot(
								propCtx, new WrapTransaction(
									productionPanel.livePanel.getTransactionFactory().getModelLocation(),
									productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), 
									selectionCreationBounds, 
									modelLocation)
							);
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
							
							ModelComponent target = productionPanel.editPanelMouseAdapter.selection;
							int addIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
							ModelComponent output = productionPanel.editPanelMouseAdapter.output;
							Location outputLocation = null;
							if(output != null)
								outputLocation = output.getTransactionFactory().getModelLocation();
								
							DualCommand<Model> dualCommand = new DualCommandPair<Model>(
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
							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().executeOnRoot(propCtx, dualCommand);
//							productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().executeOnRoot(
//								propCtx, new AddThenOutputTransaction(
//									productionPanel.livePanel.getTransactionFactory().getLocation(), 
//									productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getLocation(), 
//									creationBounds, 
//									factory)
//							);
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
						productionPanel.editPanelMouseAdapter.resetEffectFrame();
						productionPanel.livePanel.repaint();
					}
				}
				
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) { }
			});
			
			Point selectionReleasePointInSelection = SwingUtilities.convertPoint(((JComponent)(e.getSource())), e.getPoint(), productionPanel);
			factoryPopopMenu.show(productionPanel, selectionReleasePointInSelection.x + 10, selectionReleasePointInSelection.y);
		}
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == 1) {
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
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			Point selectionDragPoint = SwingUtilities.convertPoint(((JComponent)(e.getSource())).getParent(), e.getPoint(), productionPanel);
			Rectangle plotBoundsInSelection = productionPanel.editPanelMouseAdapter.getPlotBounds(productionPanel.editPanelMouseAdapter.selectionMouseDown, selectionDragPoint);
			Rectangle plotBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)productionPanel.editPanelMouseAdapter.selection, plotBoundsInSelection, productionPanel);
			productionPanel.effectFrame.setBounds(plotBoundsInProductionPanel);
			productionPanel.livePanel.repaint();
		}
	}
}
