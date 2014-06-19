package dynamake;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import javax.swing.JComponent;

import dynamake.LiveModel.LivePanel;

public class ShapeModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final ArrayList<ArrayList<Point>> pointsList;
	
	public ShapeModel(ArrayList<ArrayList<Point>> pointsList) {
		this.pointsList = pointsList;
	}

	private static class ShapeView extends JComponent implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private ShapeModel model;
		private TransactionFactory transactionFactory;
		private ArrayList<Path2D.Double> viewShapes;

		public ShapeView(ShapeModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			viewShapes = new ArrayList<Path2D.Double>();
			
			for(ArrayList<Point> points: model.pointsList) {
				Path2D.Double viewShape = new Path2D.Double();
				viewShape.moveTo(points.get(0).x, points.get(0).y);
				
				for(int i = 1; i < points.size(); i++)
					viewShape.lineTo(points.get(i).x, points.get(i).y);
				
				viewShapes.add(viewShape);
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
			for(Path2D.Double viewShape: viewShapes)
				((Graphics2D)g).draw(viewShape);
		}
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
		return new ShapeModel(new ArrayList<>(this.pointsList));
	}
}
