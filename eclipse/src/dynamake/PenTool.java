package dynamake;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;

public class PenTool implements Tool {
	@Override
	public String getName() {
		return "Pen";
	}
	
	private ArrayList<Point> points;
	private Path2D.Double shape;

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		final ArrayList<Point> pointsForCreation = points;
		// Deriving bounds for shape composition is different than the below:
		final Rectangle creationBoundsInProductionPanelSource = shape.getBounds();
		final Rectangle creationBoundsInProductionPanel = 
			new Rectangle(
				creationBoundsInProductionPanelSource.x - (int)StrokeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.y - (int)StrokeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.width + (int)StrokeModel.STROKE_SIZE * 2, 
				creationBoundsInProductionPanelSource.height + (int)StrokeModel.STROKE_SIZE * 2
		);
		
		points = null;
		shape = null;
		
		final PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		PropogationContext propCtx = new PropogationContext();
		
		final ModelComponent target = canvas;
		final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)target);
		
		branch.execute(propCtx, new DualCommandFactory<Model>() {
			@Override
			public void createDualCommands(List<DualCommand<Model>> dualCommands) {
				productionPanel.livePanel.productionPanel.editPanelMouseAdapter.createSelectCommands(null, dualCommands);
				dualCommands.add(LiveModel.SetOutput.createDualBackward(productionPanel.livePanel));
				
				CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
				Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
				int index = canvasModel.getModelCount();
				Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
//				ArrayList<ShapeModel.ShapeInfo> shapes = new ArrayList<ShapeModel.ShapeInfo>();
//				shapes.add(new ShapeModel.ShapeInfo(creationBoundsInProductionPanel.getLocation(), pointsForCreation));
				Factory factory = new StrokeModelFactory(creationBoundsInProductionPanel.getLocation(), pointsForCreation);
				// The location for Output depends on the side effect of add
				
				dualCommands.add(new DualCommandPair<Model>(
					new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInContainer, factory), 
					new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
				));
				
				dualCommands.add(LiveModel.SetOutput.createDualForward(productionPanel.livePanel, addedModelLocation));
			}
		});
		
		endMoveOver(productionPanel, new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
		
		branch.onFinished(new Runnable() {
			@Override
			public void run() {

			}
		});
		
		branch.close();
	}
	
	private ModelComponent canvas;
	
	private void initializeModelOver(ModelComponent canvas, final JComponent container, RunBuilder runBuilder) {
		this.canvas = canvas;
		addTargetFrame(canvas, container, runBuilder);
	}
	
	private void addTargetFrame(ModelComponent modelOver, final JComponent container, RunBuilder runBuilder) {
		final JPanel targetFrame = new JPanel();
		final Color color = ProductionPanel.TARGET_OVER_COLOR;
		targetFrame.setBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.BLACK, 1), 
				BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(color, 3), 
					BorderFactory.createLineBorder(Color.BLACK, 1)
				)
			)
		);
		
		Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
			((JComponent)modelOver).getParent(), ((JComponent)modelOver).getBounds(), container);
		targetFrame.setBounds(targetFrameBounds);
		targetFrame.setBackground(new Color(0, 0, 0, 0));

		runBuilder.addRunnable(new Runnable() {
			@Override
			public void run() {
				container.add(targetFrame);
			}
		});
	}
	
	private void endMoveOver(final JComponent container, Runner runner) {
		final JComponent localCanvas = (JComponent)canvas;
		runner.run(new Runnable() {
			@Override
			public void run() {
				container.remove(localCanvas);
			}
		});
		
		canvas = null;
	}
	
	private int i = 0;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		points = new ArrayList<Point>();
		shape = new Path2D.Double();
		points.add(e.getPoint());
		shape.moveTo(e.getX(), e.getY());
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		runBuilder.addRunnable(new Runnable() {
			@Override
			public void run() { }
		});
		
		initializeModelOver(ModelComponent.Util.closestCanvasModelComponent(modelOver), productionPanel, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		i++;
		System.out.println(i);
		points.add(e.getPoint());
		shape.lineTo(e.getX(), e.getY());
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		// TODO: Only repaint the area necessary here!!!
//		final Rectangle creationBoundsInProductionPanelSource = shape.getBounds();
//		final Rectangle creationBoundsInProductionPanel = 
//			new Rectangle(
//				creationBoundsInProductionPanelSource.x - (int)StrokeModel.STROKE_SIZE, 
//				creationBoundsInProductionPanelSource.y - (int)StrokeModel.STROKE_SIZE, 
//				creationBoundsInProductionPanelSource.width + (int)StrokeModel.STROKE_SIZE * 2, 
//				creationBoundsInProductionPanelSource.height + (int)StrokeModel.STROKE_SIZE * 2
//		);
		runBuilder.addRunnable(new Runnable() {
			@Override
			public void run() { }
		});//, productionPanel, creationBoundsInProductionPanel);
		
		runBuilder.execute();
	}

	@Override
	public void paint(Graphics g) {
		StrokeModel.setupGraphics(g);
		((Graphics2D)g).draw(shape);
	}
}
