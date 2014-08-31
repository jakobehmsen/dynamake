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

import javax.swing.JComponent;
import javax.swing.JFrame;

import dynamake.commands.CommandSequence;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.SetPropertyCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

public class RootModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Model content;
	
	public RootModel(Model content) {
		this.content = content;
		content.setParent(this);
	}
	
	@Override
	public Model cloneBase() {
		return new RootModel(content.cloneBase());
	}
	
	private static class FrameModel extends JFrame implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private RootModel model;
		private ModelTranscriber modelTranscriber;

		public FrameModel(RootModel model, ModelTranscriber modelTranscriber) {
			this.model = model;
			this.modelTranscriber = modelTranscriber;
			
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
		public ModelTranscriber getModelTranscriber() {
			return modelTranscriber;
		}

		@Override
		public void appendContainerTransactions(LivePanel livePanel, CompositeMenuBuilder menuBuilder, ModelComponent child) {

		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder) {

		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
			ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, CompositeMenuBuilder menuBuilder) {
		}
		
		@Override
		public void initialize() {
			((ModelComponent)getContentPane().getComponent(0)).initialize();
		}
	}
	
	private static class FieldContentLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object getChild(Object holder) {
			return ((RootModel)holder).content;
		}
		
		@Override
		public Location forForwarding() {
			return this;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof FieldContentLocation;
		}
		
		@Override
		public int hashCode() {
			return 17;
		}
	}
	
	private static class BoundsChangeHandler extends MouseAdapter implements ComponentListener {
		private RootModel rootModel;
		private ModelTranscriber modelTranscriber;
		private boolean mouseIsDown;
		private Point newLocation;
		private Dimension newSize;
		
		public BoundsChangeHandler(RootModel rootModel, ModelTranscriber modelTranscriber) {
			this.rootModel = rootModel;
			this.modelTranscriber = modelTranscriber;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			mouseIsDown = false;

			if(newLocation != null && newSize != null) {
				Connection<Model> connection = modelTranscriber.createConnection();
				
				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.startTransaction(rootModel, NewChangeTransactionHandler.class);
						
						collector.execute(new Trigger<Model>() {
							@Override
							public void run(Collector<Model> collector) {
//								ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
								
								if(newLocation != null) {
									collector.execute(new TriStatePURCommand<Model>(
										new CommandSequence<Model>(
											collector.createProduceCommand("X"),
											collector.createProduceCommand(newLocation.x),
											new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
										), 
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									));
									collector.execute(new TriStatePURCommand<Model>(
										new CommandSequence<Model>(
											collector.createProduceCommand("Y"),
											collector.createProduceCommand(newLocation.y),
											new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
										), 
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									));
									
//									pendingCommands.add(new PendingCommandState<Model>(
//										new SetPropertyCommand("X", newLocation.x),
//										new SetPropertyCommand.AfterSetProperty()
//									));
//									pendingCommands.add(new PendingCommandState<Model>(
//										new SetPropertyCommand("Y", newLocation.y),
//										new SetPropertyCommand.AfterSetProperty()
//									));
								}
								
								if(newSize != null) {
									collector.execute(new TriStatePURCommand<Model>(
										new CommandSequence<Model>(
											collector.createProduceCommand("Width"),
											collector.createProduceCommand(newSize.width),
											new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
										), 
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									));
									collector.execute(new TriStatePURCommand<Model>(
										new CommandSequence<Model>(
											collector.createProduceCommand("Y"),
											collector.createProduceCommand(newSize.height),
											new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
										), 
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()),
										new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope())
									));
										
//									pendingCommands.add(new PendingCommandState<Model>(
//										new SetPropertyCommand("Width", newSize.width),
//										new SetPropertyCommand.AfterSetProperty()
//									));
//									pendingCommands.add(new PendingCommandState<Model>(
//										new SetPropertyCommand("Height", newSize.height),
//										new SetPropertyCommand.AfterSetProperty()
//									));
								}
								
//								PendingCommandFactory.Util.executeSequence(collector, pendingCommands);
							}
						});

						collector.commitTransaction();
					}
				});
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
	public Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, final ModelTranscriber modelTranscriber) {
		this.setLocator(modelTranscriber.getModelLocator());
		
		final FrameModel view = new FrameModel(this, modelTranscriber);
		
		Model.loadComponentBounds(this, view);
		final RemovableListener removableListenerForBoundsChanges =  Model.wrapForBoundsChanges(this, view, viewManager);
		
		BoundsChangeHandler boundsChangeHandler = new BoundsChangeHandler(this, modelTranscriber);
		view.addMouseListener(boundsChangeHandler);
		view.addComponentListener(boundsChangeHandler);
		
		Integer state = (Integer)getProperty("State");
		if(state != null)
			view.setExtendedState(state);
		
		view.getContentPane().setLayout(new BorderLayout());
		final Binding<ModelComponent> contentView = content.createView(view, viewManager, modelTranscriber.extend(new Locator() {
			@Override
			public Location locate() {
				return new FieldContentLocation();
			}
		}));
		view.getContentPane().add((JComponent)contentView.getBindingTarget(), BorderLayout.CENTER);
		
		view.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				final int newState = e.getNewState();
				
				Connection<Model> connection = modelTranscriber.createConnection();
				
				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.startTransaction(RootModel.this, NewChangeTransactionHandler.class);
						
//						collector.execute(new Trigger<Model>() {
//							@Override
//							public void run(Collector<Model> collector) {
//								PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
//									new SetPropertyCommand("State", newState),
//									new SetPropertyCommand.AfterSetProperty()
//								));
//							}
//						});
						
						collector.execute(new TriStatePURCommand<Model>(
							new CommandSequence<Model>(
								collector.createProduceCommand("State"),
								collector.createProduceCommand(newState),
								new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
							), 
							new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()), // Outputs name of changed property and the previous value
							new ReversibleCommandPair<Model>(new SetPropertyCommandFromScope(), new SetPropertyCommandFromScope()) // Outputs name of changed property and the previous value
						));

						collector.commitTransaction();
					}
				});
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
