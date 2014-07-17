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

import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.PendingCommandState;
import dynamake.delegates.Action1;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
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
//						collector.execute(new DualCommandFactory<Model>() {
//							@Override
//							public Model getReference() {
//								return rootModel;
//							}
//							
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								if(newLocation != null) {
//									dualCommands.add(new DualCommandPair<Model>(
//										new Model.SetPropertyCommand(location, "X", newLocation.x),
//										new Model.SetPropertyCommand(location, "X", rootModel.getProperty("X"))
//									));
//									
//									dualCommands.add(new DualCommandPair<Model>(
//										new Model.SetPropertyCommand(location, "Y", newLocation.y),
//										new Model.SetPropertyCommand(location, "Y", rootModel.getProperty("Y"))
//									));
//								}
//								
//								if(newSize != null) {
//									dualCommands.add(new DualCommandPair<Model>(
//										new Model.SetPropertyCommand(location, "Width", newSize.width),
//										new Model.SetPropertyCommand(location, "Width", rootModel.getProperty("Width"))
//									));
//			
//									dualCommands.add(new DualCommandPair<Model>(
//										new Model.SetPropertyCommand(location, "Height", newSize.height),
//										new Model.SetPropertyCommand(location, "Height", rootModel.getProperty("Height"))
//									));
//								}
//							}
//						});
						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return rootModel;
							}
							
							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								if(newLocation != null) {
									commandStates.add(new PendingCommandState<Model>(
										new Model.SetPropertyCommand("X", newLocation.x),
										new Model.SetPropertyCommand.AfterSetProperty()
									));
									commandStates.add(new PendingCommandState<Model>(
										new Model.SetPropertyCommand("Y", newLocation.y),
										new Model.SetPropertyCommand.AfterSetProperty()
									));
								}
								
								if(newSize != null) {
									commandStates.add(new PendingCommandState<Model>(
										new Model.SetPropertyCommand("Width", newSize.width),
										new Model.SetPropertyCommand.AfterSetProperty()
									));
									commandStates.add(new PendingCommandState<Model>(
										new Model.SetPropertyCommand("Height", newSize.height),
										new Model.SetPropertyCommand.AfterSetProperty()
									));
								}
							}
						});
						collector.commit();
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
		final Binding<ModelComponent> contentView = content.createView(view, viewManager, modelTranscriber.extend(new ModelLocator() {
			@Override
			public ModelLocation locate() {
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
//						collector.execute(new DualCommandFactory<Model>() {
//							@Override
//							public Model getReference() {
//								return RootModel.this;
//							}
//							
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								Integer currentState = (Integer)RootModel.this.getProperty("State");
//								dualCommands.add(new DualCommandPair<Model>(
//									new Model.SetPropertyOnRootCommand(location, "State", newState),
//									new Model.SetPropertyOnRootCommand(location, "State", currentState)
//								));
//							}
//						});
						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return RootModel.this;
							}
							
							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
//								Integer currentState = (Integer)RootModel.this.getProperty("State");
								commandStates.add(new PendingCommandState<Model>(
									new Model.SetPropertyCommand("State", newState),
									new Model.SetPropertyCommand.AfterSetProperty()
								));
							}
						});
						collector.commit();
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
