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

public class DragTool implements Tool {
	@Override
	public String getName() {
		return "Drag";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			final PrevaylerServiceBranch<Model> branchStep2 = branch.branch();
			
			if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
				productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel.selectionFrame, e.getPoint(), targetModelComponent, branchStep2);
			} else {
				productionPanel.editPanelMouseAdapter.showPopupForSelectionObject(productionPanel.selectionFrame, e.getPoint(), null, branchStep2);
			}
			
			branch.close();

			productionPanel.editPanelMouseAdapter.targetOver = null;
			productionPanel.livePanel.repaint();
		}
	}
	
//	private PrevaylerServiceConnection<Model> connection;
	private PrevaylerServiceBranch<Model> branch;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
//			connection = productionPanel.livePanel.getTransactionFactory().createConnection();
			branch = productionPanel.livePanel.getTransactionFactory().createBranch();
//			productionPanel.livePanel.getTransactionFactory().beginTransaction();
			
			PrevaylerServiceBranch<Model> branchStep1 = branch.branch();
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();
//				
//				connection.execute(propCtx, new DualCommandFactory<Model>() {
//					public DualCommand<Model> createDualCommand() {
//						ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
//						return new DualCommandPair<Model>(
//							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
//							new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
//						);
//					}
//					
//					@Override
//					public void createDualCommands(
//							List<DualCommand<Model>> dualCommands) {
//						dualCommands.add(createDualCommand());
//					}
//				});
				
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
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true, branchStep1);
			}
			
			branchStep1.close();
		}
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e) {
		if(productionPanel.editPanelMouseAdapter.selectionMouseDown != null && productionPanel.editPanelMouseAdapter.effectFrameMoving) {
			Point mouseOverPoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent newTargetOver = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(mouseOverPoint);
			ModelComponent newTargetOverComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(newTargetOver);
			
			if(newTargetOverComponent != productionPanel.editPanelMouseAdapter.targetOver) {
				productionPanel.editPanelMouseAdapter.targetOver = newTargetOverComponent;
				
				if(productionPanel.targetFrame != null)
					productionPanel.remove(productionPanel.targetFrame);
				
				if(newTargetOverComponent != null && newTargetOverComponent != productionPanel.editPanelMouseAdapter.selection) {
					productionPanel.targetFrame = new JPanel();
					
					Color color = ProductionPanel.TARGET_OVER_COLOR;

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
			
			int width = productionPanel.effectFrame.getWidth();
			int height = productionPanel.effectFrame.getHeight();

			Point cursorLocationInProductionPanel = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			
			int x = cursorLocationInProductionPanel.x - productionPanel.editPanelMouseAdapter.selectionMouseDown.x;
			int y = cursorLocationInProductionPanel.y - productionPanel.editPanelMouseAdapter.selectionMouseDown.y;

			productionPanel.effectFrame.setBounds(new Rectangle(x, y, width, height));
			productionPanel.livePanel.repaint();
		}
	}
}
