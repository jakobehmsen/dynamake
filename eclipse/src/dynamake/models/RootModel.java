package dynamake.models;

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
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;

import dynamake.Action1;
import dynamake.DualCommand;
import dynamake.DualCommandFactory;
import dynamake.DualCommandPair;
import dynamake.PrevaylerServiceBranch;
import dynamake.TransactionFactory;
import dynamake.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;

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
		public void appendContainerTransactions(LivePanel livePanel, CompositeMenuBuilder menuBuilder, ModelComponent child, PrevaylerServiceBranch<Model> branch) {

		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder, PrevaylerServiceBranch<Model> branch) {

		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder, branch);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
			ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, CompositeMenuBuilder menuBuilder, PrevaylerServiceBranch<Model> branch) {
		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			return null;
		}
		
		@Override
		public void initialize() {
			((ModelComponent)getContentPane().getComponent(0)).initialize();
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
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
		public Location getModelComponentLocation() {
			return new ViewFieldContentLocation();
		}
	}
	
	private static class ViewFieldContentLocation implements Location {
		@Override
		public Object getChild(Object holder) {
			return ((FrameModel)holder).getContentPane().getComponent(0);
		}
	}
	
	private static class BoundsChangeHandler extends MouseAdapter implements ComponentListener {
		private RootModel rootModel;
		private TransactionFactory transactionFactory;
		private boolean mouseIsDown;
		private Point newLocation;
		private Dimension newSize;
		
		public BoundsChangeHandler(RootModel rootModel, TransactionFactory transactionFactory) {
			this.rootModel = rootModel;
			this.transactionFactory = transactionFactory;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			mouseIsDown = false;

			if(newLocation != null && newSize != null) {
				PropogationContext propCtx = new PropogationContext();
				PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
				
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					@Override
					public void createDualCommands(List<DualCommand<Model>> dualCommands) {
						if(newLocation != null) {
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "X", newLocation.x),
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "X", rootModel.getProperty("X"))
							));
							
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Y", newLocation.y),
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Y", rootModel.getProperty("Y"))
							));
						}
						
						if(newSize != null) {
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Width", newSize.width),
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Width", rootModel.getProperty("Width"))
							));
	
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Height", newSize.height),
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), "Height", rootModel.getProperty("Height"))
							));
						}
					}
				});
				
				branch.close();
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
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final FrameModel view = new FrameModel(this, transactionFactory);
		
		Model.loadComponentBounds(this, view);
		final RemovableListener removableListenerForBoundsChanges =  Model.wrapForBoundsChanges(this, view, viewManager);
		
		BoundsChangeHandler boundsChangeHandler = new BoundsChangeHandler(this, transactionFactory);
		view.addMouseListener(boundsChangeHandler);
		view.addComponentListener(boundsChangeHandler);
		
		Integer state = (Integer)getProperty("State");
		if(state != null)
			view.setExtendedState(state);
		
		view.getContentPane().setLayout(new BorderLayout());
		final Binding<ModelComponent> contentView = content.createView(view, viewManager, transactionFactory.extend(new ModelLocator() {
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

				final int newState = e.getNewState();
				PrevaylerServiceBranch<Model> branch = view.getTransactionFactory().createBranch();
				branch.execute(propCtx, new DualCommandFactory<Model>() {
					public DualCommand<Model> createDualCommand() {
						Location modelLocation = transactionFactory.getModelLocation();
						Integer currentState = (Integer)RootModel.this.getProperty("State");
						return new DualCommandPair<Model>(
							new Model.SetPropertyOnRootTransaction(modelLocation, "State", newState),
							new Model.SetPropertyOnRootTransaction(modelLocation, "State", currentState)
						);
					}
					
					@Override
					public void createDualCommands(
							List<DualCommand<Model>> dualCommands) {
						dualCommands.add(createDualCommand());
					}
				});
				branch.close();
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
