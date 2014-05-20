package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

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
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			Point releasePoint = SwingUtilities.convertPoint(productionPanel.selectionFrame, e.getPoint(), productionPanel);
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(releasePoint);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			
			if(targetModelComponent != null && productionPanel.editPanelMouseAdapter.selection != targetModelComponent) {
				Location liveModelLocation = productionPanel.livePanel.getTransactionFactory().getModelLocation();
				ModelComponent output = productionPanel.editPanelMouseAdapter.output;
				Location outputLocation = null;
				if(output != null)
					outputLocation = output.getTransactionFactory().getModelLocation();
				if(productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(targetModelComponent.getModelBehind())) {
					DualCommand<Model> dualCommand = new DualCommandPair<Model>(
						new Model.RemoveObserverThenOutputObserver(liveModelLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()),
						new SetOutputThenAddObserver(liveModelLocation, outputLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation())
					);
					
					targetModelComponent.getTransactionFactory().executeOnRoot(new PropogationContext(), dualCommand);
//					targetModelComponent.getTransactionFactory().executeOnRoot(
//						new PropogationContext(), new Model.RemoveObserverThenOutputObserver(liveModelLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()));
				} else {
					DualCommand<Model> dualCommand = new DualCommandPair<Model>(
						new Model.AddObserverThenOutputObserver(liveModelLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()),
						new SetOutputThenRemoveObserver(liveModelLocation, outputLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation())
					);

					targetModelComponent.getTransactionFactory().executeOnRoot(new PropogationContext(), dualCommand);
//					targetModelComponent.getTransactionFactory().executeOnRoot(
//						new PropogationContext(), new Model.AddObserverThenOutputObserver(liveModelLocation, productionPanel.editPanelMouseAdapter.selection.getTransactionFactory().getModelLocation(), targetModelComponent.getTransactionFactory().getModelLocation()));
				}
				
				PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT);
				productionPanel.livePanel.getTransactionFactory().commitTransaction(propCtx);
			} else {
				PropogationContext propCtx = new PropogationContext(LiveModel.TAG_CAUSED_BY_ROLLBACK);
				productionPanel.livePanel.getTransactionFactory().rollbackTransaction(propCtx);
			}

			productionPanel.editPanelMouseAdapter.resetEffectFrame();

			if(productionPanel.targetFrame != null)
				productionPanel.remove(productionPanel.targetFrame);
			
			productionPanel.editPanelMouseAdapter.targetOver = null;
			productionPanel.livePanel.repaint();
		}
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			productionPanel.livePanel.getTransactionFactory().beginTransaction();
			
			if(productionPanel.editPanelMouseAdapter.output != null) {
				PropogationContext propCtx = new PropogationContext();
				ModelLocation currentOutputLocation = productionPanel.editPanelMouseAdapter.output.getTransactionFactory().getModelLocation();
				DualCommand<Model> dualCommand = new DualCommandPair<Model>(
					new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), null),
					new SetOutput(productionPanel.livePanel.getTransactionFactory().getModelLocation(), currentOutputLocation)
				);
				productionPanel.livePanel.getTransactionFactory().executeOnRoot(propCtx, dualCommand);
			}
			
			Point pointInContentView = SwingUtilities.convertPoint((JComponent) e.getSource(), e.getPoint(), (JComponent)productionPanel.contentView.getBindingTarget());
			JComponent target = (JComponent)((JComponent)productionPanel.contentView.getBindingTarget()).findComponentAt(pointInContentView);
			ModelComponent targetModelComponent = productionPanel.editPanelMouseAdapter.closestModelComponent(target);
			if(targetModelComponent != null) {
				Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)targetModelComponent);
				productionPanel.editPanelMouseAdapter.selectFromView(targetModelComponent, referencePoint, true);
				productionPanel.livePanel.repaint();
			}
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e) {
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
					Color color = 
						productionPanel.editPanelMouseAdapter.selection.getModelBehind().isObservedBy(newTargetOverComponent.getModelBehind()) ? ProductionPanel.UNBIND_COLOR
						: ProductionPanel.BIND_COLOR;

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
