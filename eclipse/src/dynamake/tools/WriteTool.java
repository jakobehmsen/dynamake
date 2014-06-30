package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.delegates.Runner;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.ShapeModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.Factory;
import dynamake.models.factories.ShapeModelFactory;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.RepaintRunBuilder;
import dynamake.transcription.RunBuilder;
import dynamake.transcription.TranscriberBranch;

public class WriteTool implements Tool {
	@Override
	public String getName() {
		return "Write";
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
				creationBoundsInProductionPanelSource.x - ShapeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.y - ShapeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.width + ShapeModel.STROKE_SIZE * 2, 
				creationBoundsInProductionPanelSource.height + ShapeModel.STROKE_SIZE * 2
		);
		
		points = null;
		shape = null;
		
		final TranscriberBranch<Model> branch = productionPanel.livePanel.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		
		PropogationContext propCtx = new PropogationContext();
		
		if(targetIsCanvas()) {
			final ModelComponent target = getTargets().iterator().next();
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
					ArrayList<ShapeModel.ShapeInfo> shapes = new ArrayList<ShapeModel.ShapeInfo>();
					shapes.add(new ShapeModel.ShapeInfo(creationBoundsInProductionPanel.getLocation(), pointsForCreation));
					Factory factory = new ShapeModelFactory(shapes);
					// The location for Output depends on the side effect of add
					
					dualCommands.add(new DualCommandPair<Model>(
						new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInContainer, factory), 
						new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
					));
					
					dualCommands.add(LiveModel.SetOutput.createDualForward(productionPanel.livePanel, addedModelLocation));
				}
			});
		} else if(getTargets().size() > 0) {
			final ModelComponent canvasModelComponent = canvas;
			final Collection<ModelComponent> targets = getTargets();
			
			branch.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					productionPanel.livePanel.productionPanel.editPanelMouseAdapter.createSelectCommands(null, dualCommands);
					dualCommands.add(LiveModel.SetOutput.createDualBackward(productionPanel.livePanel));

					ArrayList<Rectangle> bounds = new ArrayList<Rectangle>();
					
					for(ModelComponent target: targets) {
						ShapeModel targetShape = (ShapeModel)target.getModelBehind();
						Rectangle targetShapeBoundsInContainer = new Rectangle(
							((Number)targetShape.getProperty("X")).intValue(),
							((Number)targetShape.getProperty("Y")).intValue(),
							((Number)targetShape.getProperty("Width")).intValue(),
							((Number)targetShape.getProperty("Height")).intValue()
						);
						Rectangle targetShapeBoundsInProductionPanel = SwingUtilities.convertRectangle((JComponent)canvasModelComponent, targetShapeBoundsInContainer, productionPanel);
						bounds.add(targetShapeBoundsInProductionPanel);
					}
					bounds.add(creationBoundsInProductionPanel);
					final Rectangle creationBoundsAllInProductionPanel = getBoundsForAll(bounds);

					ArrayList<ShapeModel.ShapeInfo> shapes = new ArrayList<ShapeModel.ShapeInfo>();
					
					for(ModelComponent target: targets) {
						ShapeModel targetShape = (ShapeModel)target.getModelBehind();
						for(int i = 0; i < targetShape.shapes.size(); i++) {
							ShapeModel.ShapeInfo sourceShape = targetShape.shapes.get(i);
							int xDelta = sourceShape.offset.x - creationBoundsAllInProductionPanel.x;
							int yDelta = sourceShape.offset.y - creationBoundsAllInProductionPanel.y;
							Point newOffset = new Point(sourceShape.offset.x - xDelta, sourceShape.offset.y - yDelta);
							ShapeModel.ShapeInfo shape = new ShapeModel.ShapeInfo(newOffset, sourceShape.points);
							shapes.add(shape);
						}
					}
					
					int xDelta = creationBoundsInProductionPanel.x - creationBoundsAllInProductionPanel.x;
					int yDelta = creationBoundsInProductionPanel.y - creationBoundsAllInProductionPanel.y;
					Point newOffset = new Point(creationBoundsInProductionPanel.x - xDelta, creationBoundsInProductionPanel.y - yDelta);
					shapes.add(new ShapeModel.ShapeInfo(newOffset, pointsForCreation));
					
					final Rectangle creationBoundsAllInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsAllInProductionPanel, (JComponent)canvasModelComponent);
					
					CanvasModel canvasModel = (CanvasModel)canvasModelComponent.getModelBehind();
					Location canvasModelLocation = canvasModelComponent.getTransactionFactory().getModelLocation();
					int index = canvasModel.getModelCount() - targets.size();
					Location addedModelLocation = canvasModelComponent.getTransactionFactory().extendLocation(new CanvasModel.IndexLocation(index));
					Factory factory = new ShapeModelFactory(shapes);
					// The location for Output depends on the side effect of add
					
					TreeMap<Integer, ModelComponent> targetsToRemove = new TreeMap<Integer, ModelComponent>();

					for(ModelComponent target: targets) {
						int targetShapeIndex = canvasModel.indexOfModel(target.getModelBehind());
						targetsToRemove.put(targetShapeIndex, target);
					}

					for(int targetShapeIndex: targetsToRemove.descendingKeySet()) {
						ModelComponent target = targetsToRemove.get(targetShapeIndex);
						
						ShapeModel targetShape = (ShapeModel)target.getModelBehind();
						
						Rectangle targetShapeBoundsInContainer = new Rectangle(
							((Number)targetShape.getProperty("X")).intValue(),
							((Number)targetShape.getProperty("Y")).intValue(),
							((Number)targetShape.getProperty("Width")).intValue(),
							((Number)targetShape.getProperty("Height")).intValue()
						);
	
						dualCommands.add(new DualCommandPair<Model>(
							new CanvasModel.RemoveModelTransaction(canvasModelLocation, targetShapeIndex), // Relative location
							new CanvasModel.AddModelAtTransaction(canvasModelLocation, targetShapeBoundsInContainer, new ShapeModelFactory(targetShape.shapes), targetShapeIndex)
						));
					}
					
					dualCommands.add(new DualCommandPair<Model>(
						new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsAllInContainer, factory), 
						new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
					));
					
					dualCommands.add(LiveModel.SetOutput.createDualForward(productionPanel.livePanel, addedModelLocation));
				}
			});
		}
		
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
	
	private Rectangle getBoundsForAll(ArrayList<Rectangle> bounds) {
		int minX = bounds.get(0).x;
		int minY = bounds.get(0).y;
		int maxRight = bounds.get(0).x + bounds.get(0).width;
		int maxBottom = bounds.get(0).y + bounds.get(0).height;
		
		for(Rectangle b: bounds) {
			int currentMinX = b.x;
			int currentMinY = b.y;
			int currentMaxRight = b.x + b.width;
			int currentMaxBottom = b.y + b.height;
			
			minX = Math.min(minX, currentMinX);
			minY = Math.min(minY, currentMinY);
			maxRight = Math.max(maxRight, currentMaxRight);
			maxBottom = Math.max(maxBottom, currentMaxBottom);
		}
		
		return new Rectangle(minX, minY, maxRight - minX, maxBottom - minY);
	}
	
	private ModelComponent canvas;
	private HashSet<ModelComponent> targets;
	private Hashtable<ModelComponent, JComponent> targetToTargetFrameMap;
	private boolean onlyCanvas;
	
	private Collection<ModelComponent> getTargets() {
		return targets;
	}
	
	private boolean targetIsCanvas() {
		return onlyCanvas;
	}
	
	private void initializeModelOver(ModelComponent canvas) {
		this.canvas = canvas;
		targets = new HashSet<ModelComponent>();
		targetToTargetFrameMap = new Hashtable<ModelComponent, JComponent>();
		onlyCanvas = false;
	}
	
	private void updateMoveOver(ModelComponent modelOver, final JComponent container, RunBuilder runBuilder) {
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			if(targets.size() == 0) {
				onlyCanvas = true;
				addTargetFrame(modelOver, container, runBuilder);
			}
		} else if(modelOver.getModelBehind() instanceof ShapeModel) {
			if(onlyCanvas) {
				targets.clear();
				onlyCanvas = false;
				
				for(final Map.Entry<ModelComponent, JComponent> entry: targetToTargetFrameMap.entrySet()) {
					runBuilder.addRunnable(new Runnable() {
						@Override
						public void run() {
							container.remove(entry.getValue());
						}
					});
				}
				targetToTargetFrameMap.clear();
			}
			
			if(!targets.contains(modelOver) /*and contained in the same immediate container*/) {
				ModelComponent modelOverCanvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);
				if(modelOverCanvas == canvas) {
					targets.add(modelOver);
					addTargetFrame(modelOver, container, runBuilder);
				}
			}
		}
	}
	
	private void addTargetFrame(ModelComponent modelOver, final JComponent container, RunBuilder runBuilder) {
		targets.add(modelOver);
		
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
		
		targetToTargetFrameMap.put(modelOver, targetFrame);
	}
	
	private void endMoveOver(final JComponent container, Runner runner) {
		canvas = null;
		targets = null;
		
		for(final Map.Entry<ModelComponent, JComponent> entry: targetToTargetFrameMap.entrySet()) {
			runner.run(new Runnable() {
				@Override
				public void run() {
					container.remove(entry.getValue());
				}
			});
		}
		targetToTargetFrameMap = null;
	}

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
		
		initializeModelOver(ModelComponent.Util.closestCanvasModelComponent(modelOver));
		updateMoveOver(modelOver, productionPanel, runBuilder);
		
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

		updateMoveOver(modelOver, productionPanel, runBuilder);
		
		runBuilder.execute();
	}

	@Override
	public void paint(Graphics g) {
		ShapeModel.setupGraphics(g);
		((Graphics2D)g).draw(shape);
	}
}
