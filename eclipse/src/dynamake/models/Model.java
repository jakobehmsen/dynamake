package dynamake.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JComponent;

import dynamake.commands.Command;
import dynamake.commands.ContextualTransaction;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.menubuilders.ColorMenuBuilder;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.factories.CloneDeepFactory;
import dynamake.models.factories.CloneIsolatedFactory;
import dynamake.models.factories.Factory;
import dynamake.numbers.Fraction;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberOnFlush;
import dynamake.transcription.TranscriberRunnable;

public abstract class Model implements Serializable, Observer {
	public static class TellProperty {
		public final String name;

		public TellProperty(String name) {
			this.name = name;
		}
	}

	public static class MouseDown { }
	
	public static class MouseUp { }

	public static final String PROPERTY_COLOR = "Color";
	public static final String PROPERTY_VIEW = "View";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int VIEW_APPLIANCE = 1;
	public static final int VIEW_ENGINEERING = 0;
	
	public static class Atom {
		public final Object value;

		public Atom(Object value) {
			this.value = value;
		}
	}
	
	public static class PropertyChanged {
		public final String name;
		public final Object value;
		
		public PropertyChanged(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
	public static class SetProperty {
		public final String name;
		public final Object value;
	
		public SetProperty(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
	public static class SetPropertyTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private String name;
		private Object value;
		
		public SetPropertyTransaction(Location modelLocation, String name, Object value) {
			this.modelLocation = modelLocation;
			this.name = name;
			this.value = value;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			model.setProperty(name, value, propCtx, 0, null, collector);
		}
		
		public static DualCommand<Model> createDual(Model model, String name, Object value) {
			Location modelLocation = model.getLocator().locate();
			return new DualCommandPair<Model>(
				new SetPropertyTransaction(modelLocation, name, value), 
				new SetPropertyTransaction(modelLocation, name, model.getProperty(name))
			);
		}
	}
	
	public static class SetPropertyOnRootTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private String name;
		private Object value;
		
		public SetPropertyOnRootTransaction(Location modelLocation, String name, Object value) {
			this.modelLocation = modelLocation;
			this.name = name;
			this.value = value;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			model.setProperty(name, value, propCtx, 0, null, collector);
		}
		
		public static DualCommand<Model> createDual(Model model, String name, Object value) {
			Location modelLocation = model.getLocator().locate();
			return new DualCommandPair<Model>(
				new SetPropertyOnRootTransaction(modelLocation, name, value), 
				new SetPropertyOnRootTransaction(modelLocation, name, model.getProperty(name))
			);
		}
	}
	
	public void log(ContextualTransaction<Model> ctxTransaction) {
		undoStack.add(ctxTransaction);
		redoStack.clear();
	}
	
	public static class UndoTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;

		public UndoTransaction(Location modelLocation) {
			this.modelLocation = modelLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			IsolatingCollector<Model> isolatingCollector = new IsolatingCollector<Model>(collector);
			model.undo(propCtx, prevalentSystem, isolatingCollector);
//			undoBranch.close();
		}
	}
	
	public static class RedoTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;

		public RedoTransaction(Location modelLocation) {
			this.modelLocation = modelLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);

			IsolatingCollector<Model> isolatingCollector = new IsolatingCollector<Model>(collector);
			model.redo(propCtx, prevalentSystem, isolatingCollector);
		}
	}
	
	public void undo(PropogationContext propCtx, Model prevalentSystem, IsolatingCollector<Model> isolatingCollector) {
		if(undoStack.isEmpty())
			return;
		
		ContextualTransaction<Model> ctxTransactionToUndo = undoStack.peek();
		
		// Could probably be cached somehow; perhaps during application load?
		final ArrayList<Model> affectedModels = new ArrayList<Model>();
		for(Location affectedModelLocation: ctxTransactionToUndo.affectedModelLocations) {
			Model affectedModel = (Model)affectedModelLocation.getChild(prevalentSystem);
			affectedModels.add(affectedModel);
		}
		
		for(Model affectedModel: affectedModels) {
			affectedModel.undoTill(propCtx, prevalentSystem, isolatingCollector, ctxTransactionToUndo);
			affectedModel.undoStack.pop();
		}
		ctxTransactionToUndo.transaction.executeBackwardOn(propCtx, prevalentSystem, null, isolatingCollector);

		for(Model affectedModel: affectedModels)
			affectedModel.redoStack.push(ctxTransactionToUndo);
	}
	
	private void undoTill(
		PropogationContext propCtx, Model prevalentSystem, IsolatingCollector<Model> isolatingCollector, ContextualTransaction<Model> ctxTransactionToUndoTill) {
		ContextualTransaction<Model> ctxTransactionToUndo = undoStack.peek();
		while(ctxTransactionToUndo != ctxTransactionToUndoTill) {
			ctxTransactionToUndo.transaction.executeBackwardOn(propCtx, prevalentSystem, null, isolatingCollector);
			undoStack.pop();
			ctxTransactionToUndo = undoStack.peek();
		}
	}
	
	public void redo(PropogationContext propCtx, Model prevalentSystem, IsolatingCollector<Model> isolatingCollector) {
		if(redoStack.isEmpty())
			return;
		
		ContextualTransaction<Model> ctxTransactionToRedo = redoStack.peek();
		
		// Could probably be cached somehow; perhaps during application load?
		final ArrayList<Model> affectedModels = new ArrayList<Model>();
		for(Location affectedModelLocation: ctxTransactionToRedo.affectedModelLocations) {
			Model affectedModel = (Model)affectedModelLocation.getChild(prevalentSystem);
			affectedModels.add(affectedModel);
		}

		for(Model affectedModel: affectedModels) {
			affectedModel.redoTill(propCtx, prevalentSystem, isolatingCollector, ctxTransactionToRedo);
			affectedModel.redoStack.pop();
		}
		ctxTransactionToRedo.transaction.executeForwardOn(propCtx, prevalentSystem, null, isolatingCollector);

		for(Model affectedModel: affectedModels)
			affectedModel.undoStack.push(ctxTransactionToRedo);
	}
	
	private void redoTill(PropogationContext propCtx, Model prevalentSystem, IsolatingCollector<Model> isolatingCollector, ContextualTransaction<Model> ctxTransactionToRedoTillæ) {
		ContextualTransaction<Model> ctxTransactionToRedo = redoStack.peek();
		while(ctxTransactionToRedo != ctxTransactionToRedoTillæ) {
			ctxTransactionToRedo.transaction.executeForwardOn(propCtx, prevalentSystem, null, isolatingCollector);
			redoStack.pop();
			ctxTransactionToRedo = redoStack.peek();
		}
	}
	
	public void setLocator(ModelLocator locator) {
		this.locator = locator;
	}
	
	public ModelLocator getLocator() {
		return locator;
	}
	
	private Stack<ContextualTransaction<Model>> undoStack = new Stack<ContextualTransaction<Model>>();
	private Stack<ContextualTransaction<Model>> redoStack = new Stack<ContextualTransaction<Model>>();
	private ModelLocator locator;
	protected Hashtable<String, Object> properties = new Hashtable<String, Object>();
	
	public void setProperty(String name, Object value, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		if(value != null)
			properties.put(name, value);
		else
			properties.remove(name);
		
		collector.registerAffectedModel(this);
		
		sendChanged(new PropertyChanged(name, value), propCtx, propDistance, 0, branch, collector);
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public static class AddObserver implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public AddObserver(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
			
			// TODO: Consider whether a change should be sent out here
		}
	}
	
	public static class RemoveObserver implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public RemoveObserver(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
			
			// TODO: Consider whether a change should be sent out here
		}
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		if(change instanceof SetProperty && changeDistance == 1) {
			// Side-effect
//			TranscriberBranch<Model> innerBranch = branch.branch();
			final SetProperty setProperty = (SetProperty)change;
			
			collector.execute(new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					Location modelLocation = getLocator().locate();
					Object currentValue = getProperty(setProperty.name);
					
					dualCommands.add(new DualCommandPair<Model>(
						new SetPropertyTransaction(modelLocation, setProperty.name, setProperty.value),
						new SetPropertyTransaction(modelLocation, setProperty.name, currentValue)
					));
				}
			});
//			innerBranch.close();
		} else if(change instanceof TellProperty && changeDistance == 1) {
			// Side-effect
//			TranscriberBranch<Model> innerBranch = branch.branch();
			
			TellProperty tellProperty = (TellProperty)change;
			Object value = getProperty(tellProperty.name);
			if(value != null)
				sendChanged(new Model.PropertyChanged(tellProperty.name, value), propCtx, propDistance, 0, null, collector);

//			innerBranch.close();
		} else {
			modelChanged(sender, change, propCtx, propDistance, changeDistance, branch, collector);
		}
	}
	
	protected void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {

	}
	
	public static class RemovableListener implements Binding<Model> {
		private Observer listener;
		private Model model;
		
		public RemovableListener(Observer listener, Model model) {
			this.listener = listener;
			this.model = model;
		}
		
		@Override
		public Model getBindingTarget() {
			return model;
		}
		
		@Override
		public void releaseBinding() {
			model.removeObserver(listener);
		}
		
		public static RemovableListener addObserver(Model model, Observer listener) {
			model.addObserver(listener);
			return new RemovableListener(listener, model);
		}
		
		public static Binding<Model> addAll(final Model model, final RemovableListener... removableListeners) {
			return new Binding<Model>() {
				@Override
				public Model getBindingTarget() {
					return model;
				}
				
				@Override
				public void releaseBinding() {
					for(RemovableListener rl: removableListeners)
						rl.releaseBinding();
				}
			};
		}
	}

	public abstract Binding<ModelComponent> createView(ModelComponent rootView, ViewManager viewManager, ModelTranscriber modelTranscriber);
	
	private static class ChangeHolder {
		public final Object change;
		public final PropogationContext propCtx;
		public final int propDistance;
		public final int changeDistance;

		public ChangeHolder(Object change, PropogationContext propCtx,
				int propDistance, int changeDistance) {
			this.change = change;
			this.propCtx = propCtx;
			this.propDistance = propDistance;
			this.changeDistance = changeDistance;
		}
	}
	
	private ArrayList<Observer> observers;
	private transient ArrayList<Observer> observersToAdd;
	private transient ArrayList<Observer> observersToRemove;
	private ArrayList<Observer> observees;
	private transient ArrayDeque<ChangeHolder> changeQueue;
	
	public Model() {
		observers = new ArrayList<Observer>();
		observees = new ArrayList<Observer>();
	}
	
	private void writeObject(ObjectOutputStream ous) throws IOException {
		ArrayList<Observer> observersToSerialize = new ArrayList<Observer>();
		for(Observer o: observers) {
			if(o instanceof Model)
				observersToSerialize.add(o);
		}
		ous.writeObject(observersToSerialize);
		ArrayList<Observer> observeesToSerialize = new ArrayList<Observer>();
		for(Observer o: observees) {
			if(o instanceof Model)
				observeesToSerialize.add(o);
		}
		ous.writeObject(observeesToSerialize);
		ous.writeObject(properties);
		ous.writeObject(undoStack);
		ous.writeObject(redoStack);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		observers = (ArrayList<Observer>)ois.readObject();
		observees = (ArrayList<Observer>)ois.readObject();
		properties = (Hashtable<String, Object>)ois.readObject();
		undoStack = (Stack<ContextualTransaction<Model>>)ois.readObject();
		redoStack = (Stack<ContextualTransaction<Model>>)ois.readObject();
	}

	public void setView(int view, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		setProperty(Model.PROPERTY_VIEW, view, propCtx, propDistance, branch, collector);
	}
	
	protected void sendChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		if(changeQueue != null) {
			changeQueue.add(new ChangeHolder(change, propCtx, propDistance, changeDistance));
			return;
		}
		
		changeQueue = new ArrayDeque<Model.ChangeHolder>();
		sendSingleChanged(change, propCtx, propDistance, changeDistance, branch, collector);
		while(true) {
			ChangeHolder changeHolder = changeQueue.poll();
			if(changeHolder == null)
				break;
			sendSingleChanged(changeHolder.change, changeHolder.propCtx, changeHolder.propDistance, changeHolder.changeDistance, branch, collector);
		}
		changeQueue = null;
	}
	
	protected void sendSingleChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		int nextChangeDistance = changeDistance + 1;
		int nextPropDistance = propDistance + 1;
		observersToAdd = new ArrayList<Observer>();
		observersToRemove = new ArrayList<Observer>();
		
		if(branch != null) {
//			if(!branch.isIsolated()) {
//				for(Observer observer: observers) {
//					PropogationContext propCtxBranch = propCtx.branch();
//					observer.changed(this, change, propCtxBranch, nextPropDistance, nextChangeDistance, branch, collector);
//				}
//			} else {
//				for(Observer observer: observers) {
//					if(!(observer instanceof Model)) {
//						PropogationContext propCtxBranch = propCtx.branch();
//						observer.changed(this, change, propCtxBranch, nextPropDistance, nextChangeDistance, branch, collector);
//					}
//				}
//			}
		} else {
			for(Observer observer: observers) {
				PropogationContext propCtxBranch = propCtx.branch();
				observer.changed(this, change, propCtxBranch, nextPropDistance, nextChangeDistance, branch, collector);
			}
		}
		
		for(Observer observerToAdd: observersToAdd) {
			observers.add(observerToAdd);
			observerToAdd.addObservee(this);
		}
		
		for(Observer observerToRemove: observersToRemove) {
			observers.remove(observerToRemove);
			observerToRemove.removeObservee(this);
		}
		
		observersToAdd = null;
		observersToRemove = null;
	}
	
	public void addObserver(Observer observer) {
		if(observersToAdd == null) {
			observers.add(observer);
			observer.addObservee(this);
		} else
			observersToAdd.add(observer);
	}
	
	public void removeObserver(Observer observer) {
		if(observersToRemove == null) {
			observers.remove(observer);
			observer.removeObservee(this);
		} else
			observersToRemove.add(observer);
	}
	
	public void addObservee(Observer observee) {
		observees.add(observee);
	}
	
	public void removeObservee(Observer observee) {
		observees.remove(observee);
	}

	public boolean isObservedBy(Observer observer) {
		return observers.contains(observer);
	}
	
	public static RemovableListener wrapForBoundsChanges(final Model model, final ModelComponent target, final ViewManager viewManager) {
		return RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
				if(change instanceof Model.PropertyChanged
						&& changeDistance == 1 /* And not a forwarded change */) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					final Component targetComponent = ((Component)target);
					if(propertyChanged.name.equals("X")) {
						collector.afterNextFlush(new TranscriberOnFlush<Model>() {
							@Override
							public void run(TranscriberCollector<Model> collector) {
								targetComponent.setLocation(new Point(((Number)propertyChanged.value).intValue(), targetComponent.getY()));
							}
						});
//						branch.onFinished(new Runnable() {
//							@Override
//							public void run() {
//								targetComponent.setLocation(new Point(((Number)propertyChanged.value).intValue(), targetComponent.getY()));
//							}
//						});
					} else if(propertyChanged.name.equals("Y")) {
						collector.afterNextFlush(new TranscriberOnFlush<Model>() {
							@Override
							public void run(TranscriberCollector<Model> collector) {
								targetComponent.setLocation(new Point(targetComponent.getX(), ((Number)propertyChanged.value).intValue()));
							}
						});
//						branch.onFinished(new Runnable() {
//							@Override
//							public void run() {
//								targetComponent.setLocation(new Point(targetComponent.getX(), ((Number)propertyChanged.value).intValue()));
//							}
//						});
					} else if(propertyChanged.name.equals("Width")) {
						collector.afterNextFlush(new TranscriberOnFlush<Model>() {
							@Override
							public void run(TranscriberCollector<Model> collector) {
								targetComponent.setSize(new Dimension(((Number)propertyChanged.value).intValue(), targetComponent.getHeight()));
							}
						});
//						branch.onFinished(new Runnable() {
//							@Override
//							public void run() {
//								targetComponent.setSize(new Dimension(((Number)propertyChanged.value).intValue(), targetComponent.getHeight()));
//							}
//						});
					} else if(propertyChanged.name.equals("Height")) {
						collector.afterNextFlush(new TranscriberOnFlush<Model>() {
							@Override
							public void run(TranscriberCollector<Model> collector) {
								targetComponent.setSize(new Dimension(targetComponent.getWidth(), ((Number)propertyChanged.value).intValue()));
							}
						});
//						branch.onFinished(new Runnable() {
//							@Override
//							public void run() {
//								targetComponent.setSize(new Dimension(targetComponent.getWidth(), ((Number)propertyChanged.value).intValue()));
//							}
//						});
					}
				}
			}
		});
	}
	
	public static final int COMPONENT_COLOR_BACKGROUND = 0;
	public static final int COMPONENT_COLOR_FOREGROUND = 1;
	
	public static RemovableListener wrapForComponentColorChanges(Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager, final int componentColor) {
		return RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
				if(change instanceof Model.PropertyChanged) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;

					if(propertyChanged.name.equals(PROPERTY_COLOR)) {
						switch(componentColor) {
						case COMPONENT_COLOR_BACKGROUND: {
							collector.afterNextFlush(new TranscriberOnFlush<Model>() {
								@Override
								public void run(TranscriberCollector<Model> collector) {
									targetComponent.setBackground((Color)propertyChanged.value);
								}
							});
						}
						case COMPONENT_COLOR_FOREGROUND: {
							collector.afterNextFlush(new TranscriberOnFlush<Model>() {
								@Override
								public void run(TranscriberCollector<Model> collector) {
									targetComponent.setForeground((Color)propertyChanged.value);
								}
							});
						}
						}
					}
				}
			}
		});
	}
	
	private static class MouseUpTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		
		public MouseUpTransaction(Location modelLocation) {
			this.modelLocation = modelLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);
			model.sendChanged(new MouseUp(), propCtx, 0, 0, null, collector);
		}
	}
	
	private static class MouseDownTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		
		public MouseDownTransaction(Location modelLocation) {
			this.modelLocation = modelLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, TranscriberCollector<Model> collector) {
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);
			model.sendChanged(new MouseDown(), propCtx, 0, 0, null, collector);
		}
	}
	
	public static void wrapForComponentGUIEvents(final Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager) {
		((JComponent)view).addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				TranscriberConnection<Model> connection = view.getModelTranscriber().createConnection();
				connection.trigger(new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.enlist(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
									new MouseUpTransaction(view.getModelTranscriber().getModelLocation()), 
									new MouseUpTransaction(view.getModelTranscriber().getModelLocation())
								));
							}
						});
						collector.enlistCommit();
						collector.flush();
					}
				});
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				TranscriberConnection<Model> connection = view.getModelTranscriber().createConnection();
				connection.trigger(new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.enlist(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								dualCommands.add(new DualCommandPair<Model>(
										new MouseDownTransaction(view.getModelTranscriber().getModelLocation()), 
										new MouseDownTransaction(view.getModelTranscriber().getModelLocation())
								));
							}
						});
						collector.enlistCommit();
						collector.flush();
					}
				});
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {

			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {

			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {

			}
		});
	}
	
	public static void loadComponentProperties(Model model, Component view, final int componentColor) {
		Object color = model.getProperty(Model.PROPERTY_COLOR);
		
		if(color != null) {
			switch(componentColor) {
			case COMPONENT_COLOR_BACKGROUND: {
				view.setBackground((Color)color);
			}
			case COMPONENT_COLOR_FOREGROUND: {
				view.setForeground((Color)color);
			}
			}
		}
	}
	
	public static void loadComponentBounds(Model model, Component view) {
		Number x = (Number)model.getProperty("X");
		if(x != null)
			view.setLocation(x.intValue(), view.getY());
		
		Number y = (Number)model.getProperty("Y");
		if(y != null)
			view.setLocation(view.getX(), y.intValue());
		
		Integer width = (Integer)model.getProperty("Width");
		if(width != null)
			view.setSize(width.intValue(), view.getHeight());
		
		Integer height = (Integer)model.getProperty("Height");
		if(height != null)
			view.setSize(view.getWidth(), height.intValue());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Model.RemovableListener bindProperty(Model model, final String modelPropertyName, final Action1<T> propertySetter) {
		Object value = model.getProperty(modelPropertyName);
		if(value != null)
			propertySetter.run((T)value);
		return Model.RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
				if(change instanceof Model.PropertyChanged 
					&& changeDistance == 1 /* And not a forwarded change */) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals(modelPropertyName)) {
						collector.afterNextFlush(new TranscriberOnFlush<Model>() {
							@Override
							public void run(TranscriberCollector<Model> collector) {
								propertySetter.run((T)propertyChanged.value);
							}
						});
					}
				}
			}
		});
	}
	
	public static void appendComponentPropertyChangeTransactions(final ModelComponent livePanel, final Model model, final ModelTranscriber modelTranscriber, CompositeMenuBuilder transactions, final TranscriberBranch<Model> branch) {
		transactions.addMenudBuilder("Set " + PROPERTY_COLOR, new ColorMenuBuilder((Color)model.getProperty(PROPERTY_COLOR), new Func1<Color, Object>() {
			@Override
			public Object call(final Color color) {
				return new TranscriberRunnable<Model>() {
					@Override
					public void run(TranscriberCollector<Model> collector) {
						collector.execute(new DualCommandFactory<Model>() {
							@Override
							public void createDualCommands(List<DualCommand<Model>> dualCommands) {
								Color currentColor = (Color)model.getProperty(PROPERTY_COLOR);
								dualCommands.add(new DualCommandPair<Model>(
									new Model.SetPropertyTransaction(modelTranscriber.getModelLocation(), PROPERTY_COLOR, color),
									new Model.SetPropertyTransaction(modelTranscriber.getModelLocation(), PROPERTY_COLOR, currentColor)
								));
							}
						});
					}
				};
			}
		}));
	}

	public void beRemoved() {
		modelBeRemoved();
		
		for(Observer observer: new ArrayList<Observer>(observers)) {
			observer.removeObservee(this);
		}
		for(Observer observee: new ArrayList<Observer>(observees)) {
			if(observee instanceof Model)
				((Model)observee).removeObserver(this);
		}
	}
	
	protected void modelBeRemoved() {
		
	}

	public static void appendGeneralDroppedTransactions(final ModelComponent livePanel,
			final ModelComponent dropped, final ModelComponent target, final Rectangle droppedBounds, CompositeMenuBuilder transactions, final TranscriberBranch<Model> branch) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addMenuBuilder("Clone Isolated", new TranscriberRunnable<Model>() {
				@Override
				public void run(TranscriberCollector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;

					collector.execute(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
							Location targetCanvasLocation = target.getModelTranscriber().getModelLocation();
							Factory factory = new CloneIsolatedFactory(dropped.getModelTranscriber().getModelLocation());
							dualCommands.add(new DualCommandPair<Model>(
								new CanvasModel.AddModelTransaction(targetCanvasLocation, creationBounds, factory),
								new CanvasModel.RemoveModelTransaction(targetCanvasLocation, cloneIndex)
							));
						}
					});
				}
			});
			transactions.addMenuBuilder("Clone Deep", new TranscriberRunnable<Model>() {
				@Override
				public void run(TranscriberCollector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;

					collector.execute(new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
							Location targetCanvasLocation = target.getModelTranscriber().getModelLocation();
							Factory factory = new CloneDeepFactory(dropped.getModelTranscriber().getModelLocation());
							dualCommands.add(new DualCommandPair<Model>(
								new CanvasModel.AddModelTransaction(targetCanvasLocation, creationBounds, factory),
								new CanvasModel.RemoveModelTransaction(targetCanvasLocation, cloneIndex)
							));
						}
					});
				}
			});
		}
	}

	public abstract Model modelCloneIsolated();

	protected Model modelCloneDeep(Hashtable<Model, Model> visited, HashSet<Model> contained) {
		return modelCloneIsolated();
	}

	public Model cloneIsolated() {
		Model clone = modelCloneIsolated();
		
		if(clone.properties == null)
			clone.properties = new Hashtable<String, Object>();
		// Assumed that cloning is not necessary for properties
		// I.e., all property values are immutable
		clone.properties.putAll(this.properties);
		
		return clone;
	}
	
	public Model cloneDeep() {
		Hashtable<Model, Model> sourceToCloneMap = new Hashtable<Model, Model>();
		cloneAndMap(sourceToCloneMap);
		
		for(Map.Entry<Model, Model> sourceToCloneEntry: sourceToCloneMap.entrySet()) {
			Model source = sourceToCloneEntry.getKey();
			Model clone = sourceToCloneEntry.getValue();
			
			for(Observer observer: source.observers) {
				if(observer instanceof Model) {
					Model observerClone = sourceToCloneMap.get(observer);
					if(observerClone != null) {
						// Only if observer is contained
						clone.observers.add(observerClone);
						observerClone.observees.add(clone);
					}
				}
			}
		}
		
		return sourceToCloneMap.get(this);
	}
	
	protected void addContent(HashSet<Model> contained) {
		contained.add(this);
		modelAddContent(contained);
	}
	
	protected void modelAddContent(HashSet<Model> contained) {
		
	}
	
	protected void cloneAndMap(Hashtable<Model, Model> sourceToCloneMap) {
		Model clone = cloneIsolated();
		sourceToCloneMap.put(this, clone);
	}

	public void inject(Model model) {
		for(Observer observer: this.observers) {
			if(observer instanceof Model) {
				model.addObserver(observer);
			}
		}
		
		for(Observer observee: this.observees) {
			if(observee instanceof Model) {
				((Model)observee).addObserver(model);
			}
		}
	}

	public void deject(Model model) {
		for(Observer observer: this.observers) {
			if(observer instanceof Model) {
				model.removeObserver(observer);
			}
		}
		
		for(Observer observee: this.observees) {
			if(observee instanceof Model) {
				((Model)observee).removeObserver(model);
			}
		}
	}

	public boolean conformsToView(int value) {
		Integer view = (Integer)getProperty(Model.PROPERTY_VIEW);
		if(view == null)
			view = 1;
		
		return view <= value;
	}

	public boolean viewConformsTo(int value) {
		Integer view = (Integer)getProperty(Model.PROPERTY_VIEW);
		if(view == null)
			view = 1;

		return view >= value;
	}

	public void appendScale(final Rectangle newBounds, List<DualCommand<Model>> dualCommands) {
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "X", new Fraction(newBounds.x)));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Y", new Fraction(newBounds.y)));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Width", new Fraction(newBounds.width)));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Height", new Fraction(newBounds.height)));
		
		Fraction hChange = new Fraction(newBounds.width).divide(currentWidth);
		Fraction vChange = new Fraction(newBounds.height).divide(currentHeight);

		modelAppendScale(hChange, vChange, dualCommands);
	}

	public void appendScale(final Fraction hChange, final Fraction vChange, List<DualCommand<Model>> dualCommands) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		Fraction newX = currentX.multiply(hChange);
		Fraction newY = currentY.multiply(vChange);
		Fraction newWidth = currentWidth.multiply(hChange);
		Fraction newHeight = currentHeight.multiply(vChange);
		
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "X", newX));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Y", newY));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Width", newWidth));
		dualCommands.add(SetPropertyTransaction.createDual(Model.this, "Height", newHeight));
		
		modelAppendScale(hChange, vChange, dualCommands);
	}
	
	protected void modelAppendScale(Fraction hChange, Fraction vChange, List<DualCommand<Model>> dualCommands) {
		
	}
}
