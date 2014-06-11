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
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final ModelComponent targetModelComponent = modelOver;
		
		if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
			PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			branch.close();
			
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
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

				if(productionPanel.targetFrame != null) {
					final JPanel localTargetFrame = productionPanel.targetFrame; 
					branchStep2.onFinished(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(localTargetFrame);
						}
					});
				}
				
				productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);

				branchStep2.close();
			}
		} else {
			if(targetModelComponent.getModelBehind() instanceof CanvasModel) {
				final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
				branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				productionPanel.editPanelMouseAdapter.showPopupForSelectionCons(productionPanel, e.getPoint(), targetModelComponent, branchStep2);
				branch.close();
			} else {
				final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
				branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
				productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
				branch.reject();
			}
		}

		productionPanel.editPanelMouseAdapter.targetOver = null;
		mouseDown = null;
	}
	
	private Point mouseDown;
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		
		PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
		branchStep1.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
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

		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromDefault(targetModelComponent, referencePoint, branchStep1);
		}
		
		mouseDown = e.getPoint();
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			ModelComponent newTargetOverComponent = modelOver;
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null) {
					final JPanel localTargetFrame = productionPanel.targetFrame;
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(localTargetFrame);
						}
					});
				}
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					final Color color;
					
					if(newTargetOverComponent.getModelBehind() instanceof CanvasModel) {
						color = ProductionPanel.TARGET_OVER_COLOR;
					} else {
						color = 
							productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
							: ProductionPanel.BIND_COLOR;
					}
					
					final Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
						((JComponent)newTargetOverComponent).getParent(), ((JComponent)newTargetOverComponent).getBounds(), productionPanel);
					productionPanel.targetFrame.setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(Color.BLACK, 1), 
							BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(color, 3), 
								BorderFactory.createLineBorder(Color.BLACK, 1)
							)
						)
					);

					productionPanel.targetFrame.setBounds(targetFrameBounds);
					productionPanel.targetFrame.setBackground(new Color(0, 0, 0, 0));

					final JPanel localTargetFrame = productionPanel.targetFrame;
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							productionPanel.add(localTargetFrame);
						}
					});
				}
			}
			
			final int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			final int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();
			
			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = cursorLocationInProductionPanel.x - width / 2;
			final int y = cursorLocationInProductionPanel.y - height / 2;

//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					productionPanel.editPanelMouseAdapter.changeEffectFrameDirect(new Rectangle(x, y, width, height));
//					productionPanel.livePanel.repaint();
//				}
//			});
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), runBuilder);
			
			runBuilder.execute();
		}
	}
}
