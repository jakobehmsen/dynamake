package dynamake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JComponent;

import dynamake.LiveModel.LivePanel;

public class StrokeModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final float STROKE_SIZE = 3;

	public final Dimension creationSize;
	public final Point offset;
	public final ArrayList<Point> points;
	
	public StrokeModel(Dimension creationSize, Point offset, ArrayList<Point> points) {
		this.creationSize = creationSize;
		this.offset = offset;
		this.points = points;
	}

	private static class ShapeView extends JComponent implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private StrokeModel model;
		private TransactionFactory transactionFactory;
		private Path2D.Double viewShape;
		private float strokeSize;
		private Path2D.Double viewShapeSource;
		private Image image;
		private boolean shouldRefreshViewShape;

		public ShapeView(StrokeModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;

			viewShapeSource = new Path2D.Double();
			
			Point p = model.points.get(0);
			viewShapeSource.moveTo(p.x - model.offset.x, p.y - model.offset.y);
			
			for(int i = 1; i < model.points.size(); i++) {
				p = model.points.get(i);
				viewShapeSource.lineTo(p.x - model.offset.x, p.y - model.offset.y);
			}
			
			shouldRefreshViewShape = true;
		}

		private void refreshViewShape() {
			Fraction currentWidth = (Fraction)model.getProperty("Width");
			Fraction currentHeight = (Fraction)model.getProperty("Height");
			Fraction scaleWidth = currentWidth.divide(new Fraction(model.creationSize.width));
			Fraction scaleHeight = currentHeight.divide(new Fraction(model.creationSize.height));
			viewShape = (Path2D.Double)viewShapeSource.clone();
			viewShape.transform(AffineTransform.getScaleInstance(scaleWidth.doubleValue(), scaleHeight.doubleValue()));
			
			strokeSize = scaleWidth.add(scaleHeight).divide(new Fraction(2)).floatValue() * STROKE_SIZE;
			
//			image = new BufferedImage(currentWidth.intValue(), currentHeight.intValue(), BufferedImage.TYPE_INT_ARGB);
//			Graphics2D g = (Graphics2D)image.getGraphics();
//			setupGraphics(g, strokeSize);
//			g.draw(viewShape);
//			g.dispose();
		}

		@Override
		public Model getModelBehind() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public void appendContainerTransactions(LivePanel livePanel,
				TransactionMapBuilder transactions, ModelComponent child,
				PrevaylerServiceBranch<Model> branch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(ModelComponent livePanel,
				TransactionMapBuilder transactions,
				PrevaylerServiceBranch<Model> branch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel,
				ModelComponent target, Rectangle droppedBounds,
				TransactionMapBuilder transactions,
				PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, branch);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds,
				Point dropPoint, TransactionMapBuilder transactions,
				PrevaylerServiceBranch<Model> branch) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void initialize() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(
				ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			if(shouldRefreshViewShape)
				refreshViewShape();
			
			setupGraphics(g, strokeSize);
			
			((Graphics2D)g).draw(viewShape);
//			g.drawImage(image, 0, 0, null);
		}
	}
	
	public static void setupGraphics(Graphics g) {
        setupGraphics(g, STROKE_SIZE);
	}
	
	public static void setupGraphics(Graphics g, float strokeSize) {
		Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(strokeSize));
        g2.setColor(Color.BLACK);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final ShapeView view = new ShapeView(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		final RemovableListener removableListenerForSizeChanges = RemovableListener.addObserver(this, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
				if(change instanceof Model.PropertyChanged
						&& changeDistance == 1 /* And not a forwarded change */) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals("Width") || propertyChanged.name.equals("Height")) {
						branch.onFinished(new Runnable() {
							@Override
							public void run() {
								view.shouldRefreshViewShape = true;
							}
						});
					}
				}
			}
		});
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListenerForBoundChanges.releaseBinding();
				removableListenerForSizeChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}

	@Override
	public Model modelCloneIsolated() {
		return new StrokeModel(this.creationSize, this.offset, new ArrayList<Point>(this.points));
	}
}
