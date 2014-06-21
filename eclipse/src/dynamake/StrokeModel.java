package dynamake;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JComponent;

import dynamake.LiveModel.LivePanel;

public class StrokeModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int STROKE_SIZE = 3;
	
	public static class ShapeInfo implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public final Point offset; // Offset relative to immediate container
		public final ArrayList<Point> points; // All points are relative to sketching in live panel
		
		public ShapeInfo(Point offset, ArrayList<Point> points) {
			this.offset = offset;
			this.points = points;
		}
	}

	public final Point offset;
	public final ArrayList<Point> points;
	
	public StrokeModel(Point offset, ArrayList<Point> points) {
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

		public ShapeView(StrokeModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;

			Path2D.Double viewShape = new Path2D.Double();
			
			Point p = model.points.get(0);
			viewShape.moveTo(p.x - model.offset.x, p.y - model.offset.y);
			
			for(int i = 1; i < model.points.size(); i++) {
				p = model.points.get(i);
				viewShape.lineTo(p.x - model.offset.x, p.y - model.offset.y);
			}
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
			setupGraphics(g);
			
			((Graphics2D)g).draw(viewShape);
		}
	}
	
	public static void setupGraphics(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(STROKE_SIZE));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final ShapeView view = new ShapeView(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}

	@Override
	public Model modelCloneIsolated() {
		return new StrokeModel(this.offset, new ArrayList<Point>(this.points));
	}
}
