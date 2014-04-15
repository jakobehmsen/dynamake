package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.JComponent;
import javax.swing.JFrame;

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
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Color getPrimaryColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void create(Factory factory, Rectangle creationBounds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
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

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			final TransactionFactory transactionFactory) {
		final FrameModel view = new FrameModel(this, transactionFactory);
		
		Model.loadComponentBounds(this, view);
		final RemovableListener removableListenerForBoundsChanges =  Model.wrapForBoundsChanges(this, view);
		
		view.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) { }
			
			@Override
			public void componentResized(ComponentEvent e) {
				transactionFactory.execute(new SetPropertyTransaction("Width", e.getComponent().getWidth()));
				transactionFactory.execute(new SetPropertyTransaction("Height", e.getComponent().getHeight()));
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				transactionFactory.execute(new SetPropertyTransaction("X", e.getComponent().getX()));
				transactionFactory.execute(new SetPropertyTransaction("Y", e.getComponent().getY()));
			}
			
			@Override
			public void componentHidden(ComponentEvent e) { }
		});

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
		
		view.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
//				System.out.println(e.getNewState());
				transactionFactory.execute(new SetPropertyTransaction("State", e.getNewState()));
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
