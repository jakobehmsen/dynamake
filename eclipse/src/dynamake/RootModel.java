package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
		}

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
		
		@Override
		public TransactionPublisher getObjectTransactionPublisher() {
			return new TransactionPublisher() {
				@Override
				public void appendContainerTransactions(
						TransactionMapBuilder transactions, ModelComponent child) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void appendTransactions(TransactionMapBuilder transactions) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void appendDroppedTransactions(TransactionMapBuilder transactions) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void appendDropTargetTransactions(ModelComponent dropped,
						Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
					// TODO Auto-generated method stub
					
				}
			};
		}

		@Override
		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	private static class ContentLocation implements Location {
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
//			System.out.println("mouseReleased on frame");
			mouseIsDown = false;
			
			if(newLocation != null) {
				transactionFactory.execute(new Model.SetPropertyTransaction("X", newLocation.x));
				transactionFactory.execute(new Model.SetPropertyTransaction("Y", newLocation.y));
			}
			
			if(newSize != null) {
				transactionFactory.execute(new Model.SetPropertyTransaction("Width", newSize.width));
				transactionFactory.execute(new Model.SetPropertyTransaction("Height", newSize.height));
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
//			System.out.println("mousePressed on frame");
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
		final Binding<ModelComponent> contentView = content.createView(viewManager, transactionFactory.extend(new Locator() {
			@Override
			public Location locate() {
				return new ContentLocation();
			}
		}));
		view.getContentPane().add((JComponent)contentView.getBindingTarget(), BorderLayout.CENTER);
		
//		final TransactionFactory metaTransactionFactory = transactionFactory.extend(new Model.MetaModelLocator());
		view.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
//				System.out.println(e.getNewState());
				transactionFactory.execute(new Model.SetPropertyTransaction("State", e.getNewState()));
			}
		});
		
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
