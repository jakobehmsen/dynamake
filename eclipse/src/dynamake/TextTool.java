package dynamake;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;

public class TextTool implements Tool {
	@Override
	public String getName() {
		return "Text";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, final MouseEvent e, final ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof TextModel) {
			PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					e.setSource((JComponent)modelOver);
					Point modelOverPoint = SwingUtilities.convertPoint(productionPanel, e.getPoint(), (JComponent)modelOver);
					e.translatePoint(-e.getX(), -e.getY());
					e.translatePoint(modelOverPoint.x, modelOverPoint.y);
					for(MouseListener l: ((JComponent)modelOver).getMouseListeners())
						l.mouseReleased(e);
				}
			});
			
			branch.close();
		}
	}

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, final ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof TextModel) {
			PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					e.setSource((JComponent)modelOver);
					Point modelOverPoint = SwingUtilities.convertPoint(productionPanel, e.getPoint(), (JComponent)modelOver);
					e.translatePoint(-e.getX(), -e.getY());
					e.translatePoint(modelOverPoint.x, modelOverPoint.y);
					for(MouseListener l: ((JComponent)modelOver).getMouseListeners())
						l.mousePressed(e);
				}
			});
			
			/*
			Select modelOver
			Change the selected  
			*/
			
			branch.close();
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, final ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof TextModel) {
			PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
			branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
			
			branch.onFinished(new Runnable() {
				@Override
				public void run() {
					e.setSource((JComponent)modelOver);
					Point modelOverPoint = SwingUtilities.convertPoint(productionPanel, e.getPoint(), (JComponent)modelOver);
					e.translatePoint(-e.getX(), -e.getY());
					e.translatePoint(modelOverPoint.x, modelOverPoint.y);
					for(MouseMotionListener l: ((JComponent)modelOver).getMouseMotionListeners())
						l.mouseDragged(e);
				}
			});
			
			branch.close();
		}
	}
}
