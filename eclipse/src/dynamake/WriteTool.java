package dynamake;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.LiveModel.ProductionPanel;

public class WriteTool implements Tool {
	@Override
	public String getName() {
		return "Write";
	}
	
	private ModelComponent targetModel;
	private ArrayList<Point> points;
	private Path2D.Double shape;
	private TargetPresenter targetPresenter;
	
	private int maxPointCount;
	private Hashtable<ModelComponent, Integer> targetToPointCountMap;

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
			new Rectangle(creationBoundsInProductionPanelSource.x, creationBoundsInProductionPanelSource.y, creationBoundsInProductionPanelSource.width + 1, creationBoundsInProductionPanelSource.height + 1);
		
//		for(int i = 0; i < pointsForCreation.size(); i++) {
//			Point p = pointsForCreation.get(i);
//			p = new Point(p.x - creationBoundsInProductionPanel.x, p.y - creationBoundsInProductionPanel.y);
//			pointsForCreation.set(i, p);
//		}
		
		points = null;
		shape = null;
		
//		final ModelComponent target = targetModel;
		final ModelComponent target = targetMaxPoints;
		targetModel = null;
		
		disposeTargetPointCounts();
		
		PrevaylerServiceBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		targetPresenter.reset(branch);
		targetPresenter = null;
		
		PropogationContext propCtx = new PropogationContext();
		
		if(target.getModelBehind() instanceof CanvasModel) {
			final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)target);
			
			branch.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
					Location canvasModelLocation = target.getTransactionFactory().getModelLocation();
					int index = canvasModel.getModelCount();
					Location addedModelLocation = target.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
					ArrayList<ShapeModel.ShapeInfo> shapes = new ArrayList<ShapeModel.ShapeInfo>();
//					shapes.add(pointsForCreation);
					shapes.add(new ShapeModel.ShapeInfo(creationBoundsInProductionPanel.getLocation(), pointsForCreation));
					Factory factory = new ShapeModelFactory(shapes);
					// The location for Output depends on the side effect of add
					
					dualCommands.add(new DualCommandPair<Model>(
						new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInContainer, factory), 
						new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
					));
					
					dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation));
				}
			});
		} else if(target.getModelBehind() instanceof ShapeModel) {
			// ????
//			for(int i = 0; i < pointsForCreation.size(); i++) {
//				Point p = pointsForCreation.get(i);
//				p = new Point(p.x - creationBoundsInSelection.x, p.y - creationBoundsInSelection.y);
//				pointsForCreation.set(i, p);
//			}
			
//			final ModelComponent canvasModelComponent = ModelComponent.Util.closestCanvasModelComponent(target);
//			final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)canvasModelComponent);
//			
//			ShapeModel targetShape = (ShapeModel)target.getModelBehind();
//			final ArrayList<ArrayList<Point>> pointsList = new ArrayList<ArrayList<Point>>();
//			pointsList.addAll(targetShape.pointsList);
//			pointsList.add(pointsForCreation);
//			
//			branch.execute(propCtx, new DualCommandFactory<Model>() {
//				@Override
//				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
//					CanvasModel canvasModel = (CanvasModel)canvasModelComponent.getModelBehind();
//					Location canvasModelLocation = canvasModelComponent.getTransactionFactory().getModelLocation();
//					int index = canvasModel.getModelCount();
//					Location addedModelLocation = canvasModelComponent.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
//					Factory factory = new ShapeModelFactory(pointsList);
//					// The location for Output depends on the side effect of add
//					
//					dualCommands.add(new DualCommandPair<Model>(
//						new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInContainer, factory), 
//						new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
//					));
//					
//					dualCommands.add(LiveModel.SetOutput.createDual(productionPanel.livePanel, addedModelLocation));
//				}
//			});
		}
		
		branch.onFinished(new Runnable() {
			@Override
			public void run() {

			}
		});
		
		branch.close();
	}
	
	private ModelComponent targetMaxPoints;
	
	private void initializeTargetPointCounts() {
		targetToPointCountMap = new Hashtable<ModelComponent, Integer>();
		maxPointCount = 0;
		targetMaxPoints = null;
	}
	
	private void disposeTargetPointCounts() {
		targetToPointCountMap = null;
		targetMaxPoints = null;
	}
	
	private void incrementTargetPointCount(ModelComponent target) {
		Integer targetPointCount = targetToPointCountMap.get(target);
		
		if(targetPointCount == null)
			targetPointCount = 1;
		else
			targetPointCount++;
		
		targetToPointCountMap.put(target, targetPointCount);
		
		if(targetPointCount > maxPointCount) {
			maxPointCount = targetPointCount;
			
			if(target != targetMaxPoints ) {
				targetMaxPoints = target;
			}
		}
	}

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		targetModel = modelOver;

		initializeTargetPointCounts();
		incrementTargetPointCount(modelOver);
		
		points = new ArrayList<Point>();
		shape = new Path2D.Double();
		points.add(e.getPoint());
		shape.moveTo(e.getX(), e.getY());
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		runBuilder.addRunnable(new Runnable() {
			@Override
			public void run() { }
		});
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return ProductionPanel.TARGET_OVER_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return true;
				}
			}
		);
		
		targetPresenter.update(targetMaxPoints, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) {
		points.add(e.getPoint());
		shape.lineTo(e.getX(), e.getY());
		
		RepaintRunBuilder runBuilder = new RepaintRunBuilder(productionPanel.livePanel);
		
		runBuilder.addRunnable(new Runnable() {
			@Override
			public void run() { }
		});
		
		incrementTargetPointCount(modelOver);
		
		targetPresenter.update(targetMaxPoints, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void paint(Graphics g) {
//		System.out.println("Print write: " + shape);
		
		((Graphics2D)g).draw(shape);
	}
}
