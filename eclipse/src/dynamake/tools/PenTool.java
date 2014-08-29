package dynamake.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.TriStatePURCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.StrokeModel;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.CreationBoundsFactory;
import dynamake.models.factories.ModelFactory;
import dynamake.models.factories.StrokeModelFactory;
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

public class PenTool implements Tool {
	private ArrayList<Point> points;
	
	@Override
	public void mouseReleased(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
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
		
		final StrokeComponent localStrokeComponent = strokeComponent;
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				productionPanel.remove(localStrokeComponent);
			}
		});
		
		final ModelComponent target = targetPresenter.getTargetOver();
		final Rectangle creationBoundsInContainer = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, (JComponent)target);

		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
//				ModelFactory factory = new StrokeModelFactory(creationBoundsInProductionPanel.getLocation(), pointsForCreation, creationBoundsInContainer);
//				
//				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//					new CanvasModel.AddModelCommand(new CreationBoundsFactory(new RectangleF(creationBoundsInContainer), factory)),
//					new CanvasModel.RemoveModelCommand.AfterAdd(),
//					new CanvasModel.RestoreModelCommand.AfterRemove()
//				));

				ModelFactory factory = new CreationBoundsFactory(new RectangleF(creationBoundsInContainer), 
					new StrokeModelFactory(creationBoundsInProductionPanel.getLocation(), pointsForCreation, creationBoundsInContainer));
				
				collector.execute(new TriStatePURCommand<Model>(
					new CommandSequence<Model>(Arrays.asList(
						collector.createProduceCommand(factory),
						new ReversibleCommandPair<Model>(new CanvasModel.AddModelCommand(null), new CanvasModel.RemoveModelCommand(null))
					)),
					new CommandSequence<Model>(Arrays.asList(
						new ReversibleCommandPair<Model>(new CanvasModel.DestroyModelCommand(null), /*RegenerateCommand?*/ null),
						new ReversibleCommandPair<Model>(new CanvasModel.RemoveModelCommand(null), new CanvasModel.RestoreModelCommand(null, null))
					)),
					new ReversibleCommandPair<Model>(new CanvasModel.RestoreModelCommand(null, null), new CanvasModel.RemoveModelCommand(null))
				));
			}
		});

		targetPresenter.reset(collector);
		
		collector.commitTransaction();
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
	}
	
	private StrokeComponent strokeComponent;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, final Point mousePoint) {
		ModelComponent canvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);
		collector.startTransaction(canvas.getModelBehind(), NewChangeTransactionHandler.class);
		
		points = new ArrayList<Point>();
		strokeComponent = new StrokeComponent();
		strokeComponent.setSize(productionPanel.getSize());
		
		points.add(mousePoint);
		
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				strokeComponent.shape.moveTo(mousePoint.x, mousePoint.y);
				productionPanel.add(strokeComponent);
			}
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
		
		targetPresenter.update(canvas, collector);
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, final Point mousePoint) {
		points.add(mousePoint);
		
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
				localStrokeComponent.shape.lineTo(mousePoint.x, mousePoint.y);
			}
		});
	}

	@Override
	public void rollback(final ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);

		final StrokeComponent localStrokeComponent = strokeComponent;
		collector.afterNextTrigger(new Runnable() {
			@Override
			public void run() {
				productionPanel.remove(localStrokeComponent);
			}
		});
		
		collector.rejectTransaction();
	}
}
