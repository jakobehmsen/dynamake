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
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final ModelComponent targetModelComponent = modelOver;
		
		final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
		branchStep2.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		branch.close();
		
		final JPanel targetFrame = productionPanel.targetFrame;
		if(targetFrame != null) {
			branchStep2.onFinished(new Runnable() {
				@Override
				public void run() {
					productionPanel.remove(targetFrame);
				}
			});
		}

		productionPanel.editPanelMouseAdapter.clearEffectFrameOnBranch(branchStep2);
		productionPanel.editPanelMouseAdapter.targetOver = null;
		
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

		ModelComponent targetModelComponent = modelOver;
		if(targetModelComponent != null) {
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
			productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, branchStep1);
		}
		
		mouseDown = e.getPoint();
		
		branchStep1.close();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		if(mouseDown != null) {
			RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
			
			final ModelComponent newTargetOverComponent = modelOver;
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				if(productionPanel.targetFrame != null) {
					final JPanel oldTargetFrame = productionPanel.targetFrame;
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							productionPanel.remove(oldTargetFrame);
						}
					});
				}
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					final Color color = 
						productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
						: ProductionPanel.BIND_COLOR;

					runBuilder.addRunnable(new Runnable() {
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
			
			final int width = productionPanel.editPanelMouseAdapter.getEffectFrameWidth();
			final int height = productionPanel.editPanelMouseAdapter.getEffectFrameHeight();

			Point cursorLocationInProductionPanel = e.getPoint();
			
			final int x = productionPanel.selectionFrame.getX() + (cursorLocationInProductionPanel.x - mouseDown.x);
			final int y = productionPanel.selectionFrame.getY() + (cursorLocationInProductionPanel.y - mouseDown.y);
			
			productionPanel.editPanelMouseAdapter.changeEffectFrameDirect2(new Rectangle(x, y, width, height), runBuilder);
			
			runBuilder.execute();
		}
	}
}
