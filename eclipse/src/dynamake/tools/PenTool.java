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
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberOnFlush;

public class PenTool implements Tool {
	@Override
	public String getName() {
		return "Pen";
	}
	
	private ArrayList<Point> points;
	private Path2D.Double shape;

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		
	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
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
		
		final ModelComponent target = targetPresenter.getTargetOver();
		final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)target);
		
		collector.enlist(new DualCommandFactory<Model>() {
			@Override
			public void createDualCommands(List<DualCommand<Model>> dualCommands) {
				CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
				Location canvasModelLocation = target.getModelTranscriber().getModelLocation();
				int index = canvasModel.getModelCount();
				Factory factory = new StrokeModelFactory(creationBoundsInProductionPanel.getLocation(), pointsForCreation);
				
				dualCommands.add(new DualCommandPair<Model>(
					new CanvasModel.AddModelTransaction(canvasModelLocation, creationBoundsInContainer, factory), 
					new CanvasModel.RemoveModelTransaction(canvasModelLocation, index) // Relative location
				));
			}
		});

		targetPresenter.reset(collector);
		targetPresenter = null;
		
		collector.commit();
		collector.flush();
	}
	
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberConnection<Model> connection, TranscriberCollector<Model> collector) {
		points = new ArrayList<Point>();
		shape = new Path2D.Double();
		points.add(e.getPoint());
		shape.moveTo(e.getX(), e.getY());
		
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
		collector.flush();
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, TranscriberCollector<Model> collector, TranscriberConnection<Model> connection) {
		points.add(e.getPoint());
		shape.lineTo(e.getX(), e.getY());
		
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
		collector.afterNextFlush(new TranscriberOnFlush<Model>() {
			@Override
			public void run(TranscriberCollector<Model> collector) { }
		});
		collector.flush();
	}

	@Override
	public void paint(Graphics g) {
		StrokeModel.setupGraphics(g);
		((Graphics2D)g).draw(shape);
	}

	@Override
	public void rollback(ProductionPanel productionPanel, TranscriberCollector<Model> collector) {
		targetPresenter.reset(collector);
		targetPresenter = null;
	}
}
