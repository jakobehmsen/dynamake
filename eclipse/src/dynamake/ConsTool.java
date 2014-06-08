package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;
import dynamake.LiveModel.SetOutput;

public class ConsTool implements Tool {
	@Override
	public String getName() {
		return "Cons";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
		Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
		final ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
		
		if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
			PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel.selectionFrame, e.getPoint(), targetModelComponent, branchStep2);
				branch.close();
			} else {
				if(productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					PropogationContext propCtx = new PropogationContext();
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
							Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.RemoveObserver(observableLocation, observerLocation), // Absolute location
								new Model.AddObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
						}
					});
				} else {
					PropogationContext propCtx = new PropogationContext();
					branchStep2.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							Location observableLocation = productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation();
							Location observerLocation = targetModelComponent.getTransactionFactory().getModelLocation();
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.AddObserver(observableLocation, observerLocation), // Absolute location
								new Model.RemoveObserver(observableLocation, observerLocation) // Absolute location
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, observerLocation)); // Absolute location
						}
					});
				}

				branch.close();
				
				if(productionPanel.targetFrame != null)
					productionPanel.remove(productionPanel.targetFrame);
				
				productionPanel.editPanelMouseAdapter.clearEffectFrame();
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						productionPanel.livePanel.repaint();
					}
				});
			}
		} else {
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
				
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel.selectionFrame, e.getPoint(), targetModelComponent, branchStep2);
				branch.close();
			} else {
				productionPanel.editPanelMouseAdapter.clearEffectFrame();
				branch.reject();
			}
		}

		productionPanel.editPanelMouseAdapter.targetOver = null;
		productionPanel.livePanel.repaint();
	}
	
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
		
		if(productionPanel.editPanelMouseAdapter.output != null) {
			PropogationContext propCtx = new PropogationContext();

			branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
					
					dualCommands.add(
						new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						)
					);
				}
			});
		}
		
		Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
		JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
		ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromDefault(targetModelComponent, referencePoint, branchStep1);
		}
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null) {
			Point mouseOverPoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent newTargetOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
			ModelComponent newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(newTargetOver);
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null)
					productionPanel.remove(productionPanel.targetFrame);
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					Color color;
					
					if(newTargetOverComponent.getModelBehind() instanceof CanvasModel) {
						color = ProductionPanel.TARGET_OVER_COLOR;
					} else {
						color = 
							productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
							: ProductionPanel.BIND_COLOR;
					}

					productionPanel.targetFrame.setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(Color.BLACK, 1), 
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(color, 3), 
								BorderFactory.createLineBorder(Color.BLACK, 1)
							)
						)
					);
					
					Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
						((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), productionPanel);
					productionPanel.targetFrame.setBounds(targetFrameBounds);
					productionPanel.targetFrame.setBackground(new Color(0, 0, 0, 0));
					productionPanel.add(productionPanel.targetFrame);
				}
			}
			
			final int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			final int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();
			
			Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			
			final int x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.initialEffectBounds.width / 2;
			final int y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.initialEffectBounds.height / 2;

			productionPanel.editPanelMouseAdapter.changeEffectFrame(new Rectangle(x, y, width, height));

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.livePanel.repaint();
				}
			});
		}
	}
}
