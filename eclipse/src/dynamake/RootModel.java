package dynamake;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowStateListener;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.prevayler.Transaction;

public class RootModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model content;
	
	public RootModel(Model content) {
		this.content = content;
	}
	
	@Override
	public Model modelCloneIsolated() {
		return new RootModel(content.cloneIsolated());
	}
	
	private static class FrameModel extends JFrame implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private RootModel model;
		private TransactionFactory transactionFactory;

		public FrameModel(RootModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			this.addWindowFocusListener(new WindowFocusListener() {
				@Override
				public void windowLostFocus(WindowEvent arg0) {

				}
				
				@Override
				public void windowGainedFocus(WindowEvent arg0) {
					repaint();
				}
			});
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
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {

		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {

		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {

		}

		@Override
		public Command<Model> getImplicitDropAction(ModelComponent target) {
			return null;
		}
	}
	
	private static class FieldContentLocation implements ModelLocation {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object getChild(Object holder) {
			return ((RootModel)holder).content;
		}

		@Override
		public void setChild(Object holder, Object child) {

		}

		@Override
		public Location getModelComponentLocation() {
			return new ViewFieldContentLocation();
		}
	}
	
	private static class ViewFieldContentLocation implements Location {
		@Override
		public Object getChild(Object holder) {
			return ((FrameModel)holder).getContentPane().getComponent(0);
		}
		
		@Override
		public void setChild(Object holder, Object child) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private static class BoundsChangeHandler extends MouseAdapter implements ComponentListener {
		private TransactionFactory transactionFactory;
		private boolean mouseIsDown;
		private Point newLocation;
		private Dimension newSize;
		
		public BoundsChangeHandler(TransactionFactory transactionFactory) {
			this.transactionFactory = transactionFactory;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			mouseIsDown = false;
			
			PropogationContext propCtx = new PropogationContext();
			if(newLocation != null) {
				transactionFactory.execute(propCtx, new Model.SetPropertyTransaction("X", newLocation.x));
				transactionFactory.execute(propCtx, new Model.SetPropertyTransaction("Y", newLocation.y));
			}
			
			if(newSize != null) {
				transactionFactory.execute(propCtx, new Model.SetPropertyTransaction("Width", newSize.width));
				transactionFactory.execute(propCtx, new Model.SetPropertyTransaction("Height", newSize.height));
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			mouseIsDown = true;
			newLocation = null;
			newSize = null;
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			System.out.println("mouseClicked on frame");
		}
		
		@Override
		public void componentShown(ComponentEvent e) { }
		
		@Override
		public void componentResized(ComponentEvent e) {
			if(mouseIsDown)
				newSize = e.getComponent().getSize();
		}
		
		@Override
		public void componentMoved(ComponentEvent e) {
			if(mouseIsDown)
				newLocation = e.getComponent().getLocation();
		}
		
		@Override
		public void componentHidden(ComponentEvent e) { }
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager, final TransactionFactory transactionFactory) {
		final FrameModel view = new FrameModel(this, transactionFactory);
		
		Model.loadComponentBounds(this, view);
		final RemovableListener removableListenerForBoundsChanges =  Model.wrapForBoundsChanges(this, view, viewManager);
		
//		view.addMouseListener(new MouseListener() {
//			
//			@Override
//			public void mouseReleased(MouseEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void mousePressed(MouseEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void mouseExited(MouseEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void mouseEntered(MouseEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void mouseClicked(MouseEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//		
//		view.addComponentListener(new ComponentListener() {
//			@Override
//			public void componentShown(ComponentEvent e) { }
//			
//			@Override
//			public void componentResized(ComponentEvent e) {
//				transactionFactory.execute(new SetPropertyTransaction("Width", e.getComponent().getWidth()));
//				transactionFactory.execute(new SetPropertyTransaction("Height", e.getComponent().getHeight()));
//			}
//			
//			@Override
//			public void componentMoved(ComponentEvent e) {
//				transactionFactory.execute(new SetPropertyTransaction("X", e.getComponent().getX()));
//				transactionFactory.execute(new SetPropertyTransaction("Y", e.getComponent().getY()));
//			}
//			
//			@Override
//			public void componentHidden(ComponentEvent e) { }
//		});
		
		BoundsChangeHandler boundsChangeHandler = new BoundsChangeHandler(transactionFactory);
		view.addMouseListener(boundsChangeHandler);
		view.addComponentListener(boundsChangeHandler);
		
		Integer state = (Integer)getProperty("State");
		if(state != null)
			view.setExtendedState(state);
		
		view.getContentPane().setLayout(new BorderLayout());
		final Binding<ModelComponent> contentView = content.createView(viewManager, transactionFactory.extend(new ModelLocator() {
			@Override
			public ModelLocation locate() {
				return new FieldContentLocation();
			}
		}));
		view.getContentPane().add((JComponent)contentView.getBindingTarget(), BorderLayout.CENTER);
		
		view.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				PropogationContext propCtx = new PropogationContext();
				Integer currentState = (Integer)RootModel.this.getProperty("State");
				DualCommandPair<Model> dualCommand = new DualCommandPair<Model>(
					new Model.SetPropertyTransaction("State", e.getNewState()),
					new Model.SetPropertyTransaction("State", currentState)
				);
//				transactionFactory.execute(propCtx, new Model.SetPropertyTransaction("State", e.getNewState()));
				transactionFactory.execute(propCtx, dualCommand);
			}
		});
		
		viewManager.wasCreated(view);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				contentView.releaseBinding();
				removableListenerForBoundsChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
