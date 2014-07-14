package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.StrokeModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.Factory;
import dynamake.models.factories.StrokeModelFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.DualCommandFactory;

public class PenTool implements Tool {
	@Override
	public String getName() {
		return "Pen";
	}
	
	private ArrayList<Point> points;
//	private Path2D.Double shape;

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		final ArrayList<Point> pointsForCreation = points;
		// Deriving bounds for shape composition is different than the below:
		final Rectangle creationBoundsInProductionPanelSource = strokeComponent.shape.getBounds();
		final Rectangle creationBoundsInProductionPanel = 
			new Rectangle(
				creationBoundsInProductionPanelSource.x - (int)StrokeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.y - (int)StrokeModel.STROKE_SIZE, 
				creationBoundsInProductionPanelSource.width + (int)StrokeModel.STROKE_SIZE * 2, 
				creationBoundsInProductionPanelSource.height + (int)StrokeModel.STROKE_SIZE * 2
		);
		
		points = null;
		
		final StrokeComponent localStrokeComponent = strokeComponent;
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				productionPanel.remove(localStrokeComponent);
			}
		});
		strokeComponent = null;
		
		final ModelComponent target = targetPresenter.getTargetOver();
		final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)target);
		
		collector.execute(new DualCommandFactory<Model>() {
			@Override
			public Model getReference() {
				return target.getModelBehind();
			}
			
			@Override
			public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
				CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
				int index = canvasModel.getModelCount();
				Factory factory = new StrokeModelFactory(creationBoundsInProductionPanel.getLocation(), pointsForCreation);
				
				dualCommands.add(new DualCommandPair<Model>(
					new CanvasModel.AddModelCommand(location, creationBoundsInContainer, factory), 
					new CanvasModel.RemoveModelCommand(location, index)
				));
			}
		});

		targetPresenter.reset(collector);
		targetPresenter = null;
		
		collector.commit();
	}
	
	private static class StrokeComponent extends JComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public Path2D.Double shape;

		public StrokeComponent() {
			shape = new Path2D.Double();
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			StrokeModel.setupGraphics(g);
			((Graphics2D)g).draw(shape);
		}
		
		@Override
		public void paint(Graphics g) {
			StrokeModel.setupGraphics(g);
			((Graphics2D)g).draw(shape);
		}
		
		@Override
		public void paintAll(Graphics g) {
			StrokeModel.setupGraphics(g);
			((Graphics2D)g).draw(shape);
		}
		
		@Override
		public void paintComponents(Graphics g) {
			// TODO Auto-generated method stub
			super.paintComponents(g);
		}
	}
	
	private StrokeComponent strokeComponent;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		points = new ArrayList<Point>();
		strokeComponent = new StrokeComponent();
		strokeComponent.setSize(productionPanel.getSize());
		
		points.add(e.getPoint());
		
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				strokeComponent.shape.moveTo(e.getX(), e.getY());
				productionPanel.add(strokeComponent);
			}
		});
		
		ModelComponent canvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);

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
		
		targetPresenter.update(canvas, collector);
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		points.add(e.getPoint());
		
		// TODO: Only repaint the area necessary here!!!
//		final Rectangle creationBoundsInProductionPanelSource = shape.getBounds();
//		final Rectangle creationBoundsInProductionPanel = 
//			new Rectangle(
//				creationBoundsInProductionPanelSource.x - (int)StrokeModel.STROKE_SIZE, 
//				creationBoundsInProductionPanelSource.y - (int)StrokeModel.STROKE_SIZE, 
//				creationBoundsInProductionPanelSource.width + (int)StrokeModel.STROKE_SIZE * 2, 
//				creationBoundsInProductionPanelSource.height + (int)StrokeModel.STROKE_SIZE * 2
//		);
		
		// This provokes a repaint request
		final StrokeComponent localStrokeComponent = strokeComponent;
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() { 
				localStrokeComponent.shape.lineTo(e.getX(), e.getY());
			}
		});
	}

	@Override
	public void rollback(final ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
		targetPresenter = null;

		final StrokeComponent localStrokeComponent = strokeComponent;
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				productionPanel.remove(localStrokeComponent);
			}
		});
	}
}
