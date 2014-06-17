package dynamake;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;

public class WriteTool implements Tool {
	@Override
	public String getName() {
		return "Write";
	}
	
//	private ArrayList<Point> shape;
	private Path2D.Double shape;

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		shape = null;
		
		PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		branch.onFinished(new Runnable() {
			@Override
			public void run() {

			}
		});
		
		branch.close();
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		shape = new Path2D.Double();
		
		shape.moveTo(e.getX(), e.getY());
		
		PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		branch.onFinished(new Runnable() {
			@Override
			public void run() {

			}
		});
		
		branch.close();
	}

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		shape.lineTo(e.getX(), e.getY());
		
		PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		branch.onFinished(new Runnable() {
			@Override
			public void run() {

			}
		});
		
		branch.close();
	}

	@Override
	public void paint(Graphics g) {
//		System.out.println("Print write: " + shape);
		
		((Graphics2D)g).draw(shape);
	}
}
