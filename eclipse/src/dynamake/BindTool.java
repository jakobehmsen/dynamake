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

public class BindTool implements Tool {
	@Override
	public String getName() {
		return "Bind";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			final ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			branch.close();
			
			if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
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
				
				branchStep2.close();
			} else {
				branchStep2.reject();
			}
			
			productionPanel.editPanelMouseAdapter.targetOver = null;

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.editPanelMouseAdapter.resetEffectFrame();

					if(productionPanel.targetFrame != null)
						productionPanel.remove(productionPanel.targetFrame);
					
					productionPanel.livePanel.repaint();
				}
			});
		}
	}
	
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			
			PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();

				branchStep1.execute(propCtx, new DualCommandFactory<Model>() {
					public DualCommand<Model> createDualCommand() {
						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
						return new DualCommandPair<Model>(
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
						);
					}
					
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(createDualCommand());
					}
				});
			}
			
			Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			if(targetModelComponent != null) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true, branchStep1);
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						productionPanel.livePanel.repaint();
					}
				});
			}
			
			branchStep1.close();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null && productionPanel.editPanelMouseAdapter.effectFrameMoving) {
			Point mouseOverPoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent newTargetOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
			final ModelComponent newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(newTargetOver);
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(productionPanel.targetFrame);
						}
					});
				}
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					final Color color = 
						productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
						: ProductionPanel.BIND_COLOR;

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
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
					});
				}
			}
			
			final int width = productionPanel.effectFrame.getWidth();
			final int height = productionPanel.effectFrame.getHeight();

			Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			
			final int x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
			final int y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					productionPanel.effectFrame.setBounds(new Rectangle(x, y, width, height));
					productionPanel.livePanel.repaint();
				}
			});
		}
	}
}
