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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JComponent;

import dynamake.commands.Command;
import dynamake.commands.Command2;
import dynamake.commands.Command2Factory;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.PendingCommandState;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.menubuilders.ColorMenuBuilder;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.factories.CloneDeepFactory;
import dynamake.models.factories.CloneIsolatedFactory;
import dynamake.models.factories.Factory;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.IsolatingCollector;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

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
	
	public static class SetPropertyCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private String name;
		private Object value;
		
		public SetPropertyCommand(Location modelLocation, String name, Object value) {
			this.modelLocation = modelLocation;
			this.name = name;
			this.value = value;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			model.setProperty(name, value, propCtx, 0, collector);
		}
		
		public static DualCommand<Model> createDual(Model model, String name, Object value) {
			Location modelLocation = model.getLocator().locate();
			return new DualCommandPair<Model>(
				new SetPropertyCommand(modelLocation, name, value), 
				new SetPropertyCommand(modelLocation, name, model.getProperty(name))
			);
		}
		
		public static DualCommand<Model> createDual(Location modelLocation, Model model, String name, Object value) {
			return new DualCommandPair<Model>(
				new SetPropertyCommand(modelLocation, name, value), 
				new SetPropertyCommand(modelLocation, name, model.getProperty(name))
			);
		}
	}
	
	public static class SetPropertyCommand2 implements Command2<Model> {
		public static class AfterSetProperty implements Command2Factory<Model> {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Command2<Model> createCommand(Object output) {
				SetPropertyCommand2.Output setPropertyOutput = (SetPropertyCommand2.Output)output;
				return new SetPropertyCommand2(setPropertyOutput.name, setPropertyOutput.previousValue);
			}
		}
		
		public static class Output implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final String name;
			public final Object previousValue;
			
			public Output(String name, Object previousValue) {
				this.name = name;
				this.previousValue = previousValue;
			}
		}
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String name;
		private Object value;
		
		public SetPropertyCommand2(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(prevalentSystem);
			
			Object previousValue = model.getProperty(name);
			model.setProperty(name, value, propCtx, 0, collector);
			
			return new Output(name, previousValue);
		}
		
		public static DualCommand<Model> createDual(Model model, String name, Object value) {
			Location modelLocation = model.getLocator().locate();
			return new DualCommandPair<Model>(
				new SetPropertyCommand(modelLocation, name, value), 
				new SetPropertyCommand(modelLocation, name, model.getProperty(name))
			);
		}
		
		public static DualCommand<Model> createDual(Location modelLocation, Model model, String name, Object value) {
			return new DualCommandPair<Model>(
				new SetPropertyCommand(modelLocation, name, value), 
				new SetPropertyCommand(modelLocation, name, model.getProperty(name))
			);
		}
	}
	
	public static class SetPropertyOnRootCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location modelLocation;
		private String name;
		private Object value;
		
		public SetPropertyOnRootCommand(Location modelLocation, String name, Object value) {
			this.modelLocation = modelLocation;
			this.name = name;
			this.value = value;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			model.setProperty(name, value, propCtx, 0, collector);
		}
		
		public static DualCommand<Model> createDual(Model model, String name, Object value) {
			Location modelLocation = model.getLocator().locate();
			return new DualCommandPair<Model>(
				new SetPropertyOnRootCommand(modelLocation, name, value), 
				new SetPropertyOnRootCommand(modelLocation, name, model.getProperty(name))
			);
		}
	}
	
	public void log(DualCommand<Model> transactionFromReference) {
//		undoStack.add(transactionFromReference);
//		redoStack.clear();
	}
	
	public static class UndoCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private boolean isolate;

		public UndoCommand(Location modelLocation, boolean isolate) {
			this.modelLocation = modelLocation;
			this.isolate = isolate;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			if(isolate)
				collector = new IsolatingCollector<Model>(collector);
			
			model.undo(propCtx, prevalentSystem, collector);
		}
	}
	
	public static class UndoCommand2 implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean isolate;

		public UndoCommand2(boolean isolate) {
			this.isolate = isolate;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(prevalentSystem);
			
			if(isolate)
				collector = new IsolatingCollector<Model>(collector);
			
			model.undo(propCtx, prevalentSystem, collector);
			
			return null;
		}
	}
	
	public static class RedoCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location modelLocation;
		private boolean isolate;

		public RedoCommand(Location modelLocation, boolean isolate) {
			this.modelLocation = modelLocation;
			this.isolate = isolate;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			if(isolate)
				collector = new IsolatingCollector<Model>(collector);
			
			model.redo(propCtx, prevalentSystem, collector);
		}
	}
	
	public static class RedoCommand2 implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean isolate;

		public RedoCommand2(boolean isolate) {
			this.isolate = isolate;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(prevalentSystem);
			
			if(isolate)
				collector = new IsolatingCollector<Model>(collector);
			
			model.redo(propCtx, prevalentSystem, collector);
			
			return null;
		}
	}
	
	public void undo(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector) {
		if(undoStack.isEmpty())
			return;
		
		CommandState<Model> ctxTransactionToUndo = undoStack.pop();

		CommandState<Model> redoable = ctxTransactionToUndo.executeOn(propCtx, this, null, collector, new ModelRootLocation());

		redoStack.push(redoable);
	}
	
	public void redo(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector) {
		if(redoStack.isEmpty())
			return;
		
		CommandState<Model> ctxTransactionToRedo = redoStack.pop();

		CommandState<Model> undoable = ctxTransactionToRedo.executeOn(propCtx, this, null, collector, new ModelRootLocation());

		undoStack.push(undoable);
	}

	public void log(CommandState<Model> transactionFromReference) {
		undoStack.add(transactionFromReference);
		redoStack.clear();
	}
	
	public void setLocator(ModelLocator locator) {
		this.locator = locator;
	}
	
	public ModelLocator getLocator() {
		return locator;
	}
	
	protected Stack<CommandState<Model>> undoStack = new Stack<CommandState<Model>>();
	protected Stack<CommandState<Model>> redoStack = new Stack<CommandState<Model>>();
	private ModelLocator locator;
	protected Hashtable<String, Object> properties = new Hashtable<String, Object>();
	
	public void setProperty(String name, Object value, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(value != null)
			properties.put(name, value);
		else
			properties.remove(name);
		
		collector.registerAffectedModel(this);
		
		sendChanged(new PropertyChanged(name, value), propCtx, propDistance, 0, collector);
	}
	
	public void setBounds(RectangleF bounds, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		setProperty("X", bounds.x, propCtx, propDistance, collector);
		setProperty("Y", bounds.y, propCtx, propDistance, collector);
		setProperty("Width", bounds.width, propCtx, propDistance, collector);
		setProperty("Height", bounds.height, propCtx, propDistance, collector);
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public RectangleF getBounds() {
		Fraction x = (Fraction)getProperty("X");
		Fraction y = (Fraction)getProperty("Y");
		Fraction width = (Fraction)getProperty("Width");
		Fraction height = (Fraction)getProperty("Height");
		
		return new RectangleF(x, y, width, height);
	}
	
	public static class AddObserverCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public AddObserverCommand(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
			
			// TODO: Consider whether a change should be sent out here
		}
	}

	public static class AddObserverCommand2 implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public AddObserverCommand2(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model observable = (Model)new CompositeModelLocation((ModelLocation)location, (ModelLocation)observableLocation).getChild(rootPrevalentSystem);
			Model observer = (Model)new CompositeModelLocation((ModelLocation)location, (ModelLocation)observerLocation).getChild(rootPrevalentSystem);
			
//			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
//			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
			System.out.println(observer + " now observes " + observable);
			
			// TODO: Consider whether a change should be sent out here
			return null;
		}
	}
	
	public static class RemoveObserverCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public RemoveObserverCommand(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
			
			// TODO: Consider whether a change should be sent out here
		}
	}

	public static class RemoveObserverCommand2 implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location observableLocation;
		private Location observerLocation;
		
		public RemoveObserverCommand2(Location observableLocation, Location observerLocation) {
			this.observableLocation = observableLocation;
			this.observerLocation = observerLocation;
		}

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model observable = (Model)new CompositeModelLocation((ModelLocation)location, (ModelLocation)observableLocation).getChild(rootPrevalentSystem);
			Model observer = (Model)new CompositeModelLocation((ModelLocation)location, (ModelLocation)observerLocation).getChild(rootPrevalentSystem);
			
//			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
//			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
			System.out.println(observer + " no longer observes " + observable);
			
			// TODO: Consider whether a change should be sent out here
			return null;
		}
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof SetProperty && changeDistance == 1) {
			// Side-effect
			final SetProperty setProperty = (SetProperty)change;

//			collector.execute(new DualCommandFactory<Model>() {
//				@Override
//				public Model getReference() {
//					return Model.this;
//				}
//				
//				@Override
//				public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//					Object currentValue = getProperty(setProperty.name);
//					
//					dualCommands.add(new DualCommandPair<Model>(
//						new SetPropertyCommand(location, setProperty.name, setProperty.value),
//						new SetPropertyCommand(location, setProperty.name, currentValue)
//					));
//				}
//			});
			
			collector.execute(new CommandStateFactory<Model>() {
				@Override
				public Model getReference() {
					return Model.this;
				}
				
				@Override
				public void createDualCommands(List<CommandState<Model>> commandStates) {
//					Object currentValue = getProperty(setProperty.name);
					
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyCommand2(setProperty.name, setProperty.value),
						new SetPropertyCommand2.AfterSetProperty()
					));
				}
			});
		} else if(change instanceof TellProperty && changeDistance == 1) {
			// Side-effect
			TellProperty tellProperty = (TellProperty)change;
			Object value = getProperty(tellProperty.name);
			if(value != null)
				sendChanged(new Model.PropertyChanged(tellProperty.name, value), propCtx, propDistance, 0, collector);
		} else {
			modelChanged(sender, change, propCtx, propDistance, changeDistance, collector);
		}
	}
	
	protected void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {

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
	
	private ArrayList<Observer> observers;
	private ArrayList<Observer> observees;
	
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
		undoStack = (Stack<CommandState<Model>>)ois.readObject();
		redoStack = (Stack<CommandState<Model>>)ois.readObject();
	}

	public void setView(int view, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		setProperty(Model.PROPERTY_VIEW, view, propCtx, propDistance, collector);
	}
	
	protected void sendChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		int nextChangeDistance = changeDistance + 1;
		int nextPropDistance = propDistance + 1;
		
		for(Observer observer: observers) {
			PropogationContext propCtxBranch = propCtx.branch();
			observer.changed(this, change, propCtxBranch, nextPropDistance, nextChangeDistance, collector);
		}
	}
	
	public void addObserver(Observer observer) {
		observers.add(observer);
		observer.addObservee(this);
	}
	
	public void removeObserver(Observer observer) {
		observers.remove(observer);
		observer.removeObservee(this);
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
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
				if(change instanceof Model.PropertyChanged
						&& changeDistance == 1 /* And not a forwarded change */) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					final Component targetComponent = ((Component)target);
					if(propertyChanged.name.equals("X")) {
						collector.afterNextTrigger(new Runnable() {
							@Override
							public void run() {
								targetComponent.setLocation(new Point(((Number)propertyChanged.value).intValue(), targetComponent.getY()));
							}
						});
					} else if(propertyChanged.name.equals("Y")) {
						collector.afterNextTrigger(new Runnable() {
							@Override
							public void run() {
								targetComponent.setLocation(new Point(targetComponent.getX(), ((Number)propertyChanged.value).intValue()));
							}
						});
					} else if(propertyChanged.name.equals("Width")) {
						collector.afterNextTrigger(new Runnable() {
							@Override
							public void run() {
								targetComponent.setSize(new Dimension(((Number)propertyChanged.value).intValue(), targetComponent.getHeight()));
							}
						});
					} else if(propertyChanged.name.equals("Height")) {
						collector.afterNextTrigger(new Runnable() {
							@Override
							public void run() {
								targetComponent.setSize(new Dimension(targetComponent.getWidth(), ((Number)propertyChanged.value).intValue()));
							}
						});
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
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
				if(change instanceof Model.PropertyChanged) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;

					if(propertyChanged.name.equals(PROPERTY_COLOR)) {
						switch(componentColor) {
						case COMPONENT_COLOR_BACKGROUND: {
							collector.afterNextTrigger(new Runnable() {
								@Override
								public void run() {
									targetComponent.setBackground((Color)propertyChanged.value);
									System.out.println("Change background");
								}
							});
							break;
						}
						case COMPONENT_COLOR_FOREGROUND: {
							collector.afterNextTrigger(new Runnable() {
								@Override
								public void run() {
									targetComponent.setForeground((Color)propertyChanged.value);
								}
							});
							break;
						}
						}
					}
				}
			}
		});
	}
	
	private static class MouseUpCommand implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(rootPrevalentSystem);
			model.sendChanged(new MouseUp(), propCtx, 0, 0, collector);
			
			return null;
		}
	}
	
	private static class MouseDownCommand implements Command2<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(rootPrevalentSystem);
			model.sendChanged(new MouseDown(), propCtx, 0, 0, collector);
			
			return null;
		}
	}
	
	public static void wrapForComponentGUIEvents(final Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager) {
		((JComponent)view).addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				Connection<Model> connection = view.getModelTranscriber().createConnection();
//				connection.trigger(new Trigger<Model>() {
//					@Override
//					public void run(Collector<Model> collector) {
//						collector.execute(new DualCommandFactory<Model>() {
//							@Override
//							public Model getReference() {
//								return view.getModelBehind();
//							}
//
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								dualCommands.add(new DualCommandPair<Model>(
//									new MouseUpCommand(location), 
//									new MouseUpCommand(location)
//								));
//							}
//						});
//						collector.commit();
//					}
//				});
				
				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return view.getModelBehind();
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								commandStates.add(new PendingCommandState<Model>(
									new MouseUpCommand(), 
									new Command2.Null<Model>()
								));
							}
						});
						collector.commit();
					}
				});
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				Connection<Model> connection = view.getModelTranscriber().createConnection();
//				connection.trigger(new Trigger<Model>() {
//					@Override
//					public void run(Collector<Model> collector) {
//						collector.execute(new DualCommandFactory<Model>() {
//							@Override
//							public Model getReference() {
//								return view.getModelBehind();
//							}
//
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								dualCommands.add(new DualCommandPair<Model>(
//									new MouseDownCommand(location), 
//									new MouseDownCommand(location)
//								));
//							}
//						});
//						collector.commit();
//					}
//				});
				
				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return view.getModelBehind();
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> commandStates) {
								commandStates.add(new PendingCommandState<Model>(
									new MouseDownCommand(), 
									new Command2.Null<Model>()
								));
							}
						});
						collector.commit();
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
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
				if(change instanceof Model.PropertyChanged 
					&& changeDistance == 1 /* And not a forwarded change */) {
					final Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals(modelPropertyName)) {
						collector.afterNextTrigger(new Runnable() {
							@Override
							public void run() {
								propertySetter.run((T)propertyChanged.value);
							}
						});
					}
				}
			}
		});
	}
	
	public static void appendComponentPropertyChangeTransactions(final ModelComponent livePanel, final Model model, final ModelTranscriber modelTranscriber, CompositeMenuBuilder transactions) {
		transactions.addMenuBuilder("Set " + PROPERTY_COLOR, new ColorMenuBuilder((Color)model.getProperty(PROPERTY_COLOR), new Func1<Color, Object>() {
			@Override
			public Object call(final Color color) {
				return new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
//						collector.execute(new DualCommandFactory<Model>() {
//							@Override
//							public Model getReference() {
//								return model;
//							}
//
//							@Override
//							public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//								Color currentColor = (Color)model.getProperty(PROPERTY_COLOR);
//								dualCommands.add(new DualCommandPair<Model>(
//									new Model.SetPropertyCommand(location, PROPERTY_COLOR, color),
//									new Model.SetPropertyCommand(location, PROPERTY_COLOR, currentColor)
//								));
//							}
//						});
						
						collector.execute(new CommandStateFactory<Model>() {
							@Override
							public Model getReference() {
								return model;
							}

							@Override
							public void createDualCommands(List<CommandState<Model>> dualCommands) {
//								Color currentColor = (Color)model.getProperty(PROPERTY_COLOR);
								dualCommands.add(new PendingCommandState<Model>(
									new Model.SetPropertyCommand2(PROPERTY_COLOR, color),
									new Model.SetPropertyCommand2.AfterSetProperty()
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
			final ModelComponent dropped, final ModelComponent target, final Rectangle droppedBounds, CompositeMenuBuilder transactions) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addMenuBuilder("Clone Isolated", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;

//					collector.execute(new DualCommandFactory<Model>() {
//						@Override
//						public Model getReference() {
//							return target.getModelBehind();
//						}
//
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
//							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
//							ModelLocation fromTargetToCCA = ModelComponent.Util.locationToAncestor((ModelLocation)location, cca, target);
//							ModelLocation fromCCAToDropped = ModelComponent.Util.locationFromAncestor(fromTargetToCCA, cca, dropped);
//							// Probably, the "version" of dropped to be cloned is important
//							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
//							Location droppedLocation = fromCCAToDropped;
//							Factory factory = new CloneIsolatedFactory(droppedLocation);
//							dualCommands.add(new DualCommandPair<Model>(
//								new CanvasModel.AddModelCommand(location, creationBounds, factory),
//								new CanvasModel.RemoveModelCommand(location, cloneIndex)
//							));
//						}
//					});
					
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return target.getModelBehind();
						}

						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
//							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							ModelLocation fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							ModelLocation fromCCAToDropped = new CompositeModelLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromCCAToDropped;
							Factory factory = new CloneIsolatedFactory(droppedLocation);
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand2(creationBounds, factory),
								new CanvasModel.RemoveModelCommand2.AfterAdd(),
								new CanvasModel.AddModelCommand2.AfterRemove()
							));
						}
					});
				}
			});
			transactions.addMenuBuilder("Clone Deep", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;

//					collector.execute(new DualCommandFactory<Model>() {
//						@Override
//						public Model getReference() {
//							return target.getModelBehind();
//						}
//
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
//							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
//							ModelLocation fromTargetToCCA = ModelComponent.Util.locationToAncestor((ModelLocation)location, cca, target);
//							ModelLocation fromCCAToDropped = ModelComponent.Util.locationFromAncestor(fromTargetToCCA, cca, dropped);
//							// Probably, the "version" of dropped to be cloned is important
//							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
//							Location droppedLocation = fromCCAToDropped;
//							Factory factory = new CloneDeepFactory(droppedLocation);
//							dualCommands.add(new DualCommandPair<Model>(
//								new CanvasModel.AddModelCommand(location, creationBounds, factory),
//								new CanvasModel.RemoveModelCommand(location, cloneIndex)
//							));
//						}
//					});
					
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return target.getModelBehind();
						}

						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
//							int cloneIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							ModelLocation fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							ModelLocation fromCCAToDropped = new CompositeModelLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromCCAToDropped;
							Factory factory = new CloneDeepFactory(droppedLocation);
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand2(creationBounds, factory),
								new CanvasModel.RemoveModelCommand2.AfterAdd(),
								new CanvasModel.AddModelCommand2.AfterRemove()
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
		
		clone.undoStack.addAll(this.undoStack);
		clone.redoStack.addAll(this.redoStack);
		
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

	public void resize(Fraction xDelta, Fraction tDelta, Fraction widthDelta, Fraction heightDelta, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction newX = currentX.add(xDelta);
		Fraction newY = currentY.add(tDelta);

		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		Fraction newWidth = currentWidth.add(widthDelta);
		Fraction newHeight = currentHeight.add(heightDelta);
		
		setProperty("X", newX, propCtx, propDistance, collector);
		setProperty("Y", newY, propCtx, propDistance, collector);
		setProperty("Width", newWidth, propCtx, propDistance, collector);
		setProperty("Height", newHeight, propCtx, propDistance, collector);
	}

	public void scale(Fraction xDelta, Fraction tDelta, Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction newX = currentX.add(xDelta);
		Fraction newY = currentY.add(tDelta);

		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		Fraction newWidth = currentWidth.multiply(hChange);
		Fraction newHeight = currentHeight.multiply(vChange);
		
		setProperty("X", newX, propCtx, propDistance, collector);
		setProperty("Y", newY, propCtx, propDistance, collector);
		setProperty("Width", newWidth, propCtx, propDistance, collector);
		setProperty("Height", newHeight, propCtx, propDistance, collector);

		modelScale(hChange, vChange, propCtx, propDistance, collector);
	}
	
	public void scale(final Fraction hChange, final Fraction vChange, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		Fraction newX = currentX.multiply(hChange);
		Fraction newY = currentY.multiply(vChange);
		Fraction newWidth = currentWidth.multiply(hChange);
		Fraction newHeight = currentHeight.multiply(vChange);
		
		setProperty("X", newX, propCtx, propDistance, collector);
		setProperty("Y", newY, propCtx, propDistance, collector);
		setProperty("Width", newWidth, propCtx, propDistance, collector);
		setProperty("Height", newHeight, propCtx, propDistance, collector);

		modelScale(hChange, vChange, propCtx, propDistance, collector);
	}
	
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		
	}

	public void appendScale(ModelLocation location, final Rectangle newBounds, List<DualCommand<Model>> dualCommands) {
		// TODO: Consider the following
		// What if one or more of the contained models are removed and, afterwards, an undone of the scale is requested?
		// I.e., one or more of the embedded scale transaction will fail in the current setup.
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "X", new Fraction(newBounds.x)));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Y", new Fraction(newBounds.y)));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Width", new Fraction(newBounds.width)));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Height", new Fraction(newBounds.height)));
		
		Fraction hChange = new Fraction(newBounds.width).divide(currentWidth);
		Fraction vChange = new Fraction(newBounds.height).divide(currentHeight);

		modelAppendScale(location, hChange, vChange, dualCommands);
	}

	public void appendScale(ModelLocation location, final Fraction hChange, final Fraction vChange, List<DualCommand<Model>> dualCommands) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		Fraction newX = currentX.multiply(hChange);
		Fraction newY = currentY.multiply(vChange);
		Fraction newWidth = currentWidth.multiply(hChange);
		Fraction newHeight = currentHeight.multiply(vChange);
		
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "X", newX));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Y", newY));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Width", newWidth));
		dualCommands.add(SetPropertyCommand.createDual(location, Model.this, "Height", newHeight));
		
		modelAppendScale(location, hChange, vChange, dualCommands);
	}
	
	protected void modelAppendScale(ModelLocation location, Fraction hChange, Fraction vChange, List<DualCommand<Model>> dualCommands) {
		
	}

	public static void executeRemoveObserver(Collector<Model> collector, final ModelComponent observable, final ModelComponent observer) {
//		collector.execute(new DualCommandFactory<Model>() {
//			ModelComponent referenceMC;
//			
//			@Override
//			public Model getReference() {
//				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
//				return referenceMC.getModelBehind();
//			}
//			
//			@Override
//			public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//				ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, observable);
//				ModelLocation observerLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, observer);
//				
//				dualCommands.add(new DualCommandPair<Model>(
//					new Model.RemoveObserverCommand(observableLocation, observerLocation),
//					new Model.AddObserverCommand(observableLocation, observerLocation)
//				));
//			}
//		});
		
		collector.execute(new CommandStateFactory<Model>() {
			ModelComponent referenceMC;
			
			@Override
			public Model getReference() {
				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
				return referenceMC.getModelBehind();
			}
			
			@Override
			public void createDualCommands(List<CommandState<Model>> commandStates) {
				ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				ModelLocation observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);
				
				commandStates.add(new PendingCommandState<Model>(
					new Model.RemoveObserverCommand2(observableLocation, observerLocation),
					new Model.AddObserverCommand2(observableLocation, observerLocation)
				));
			}
		});
	}

	public static void executeAddObserver(Collector<Model> collector, final ModelComponent observable, final ModelComponent observer) {
//		collector.execute(new DualCommandFactory<Model>() {
//			ModelComponent referenceMC;
//			
//			@Override
//			public Model getReference() {
//				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
//				return referenceMC.getModelBehind();
//			}
//			
//			@Override
//			public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//				ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, observable);
//				ModelLocation observerLocation = ModelComponent.Util.locationFromAncestor((ModelLocation)location, referenceMC, observer);
//				
//				dualCommands.add(new DualCommandPair<Model>(
//					new Model.AddObserverCommand(observableLocation, observerLocation),
//					new Model.RemoveObserverCommand(observableLocation, observerLocation)
//				));
//			}
//		});
		
		collector.execute(new CommandStateFactory<Model>() {
			ModelComponent referenceMC;
			
			@Override
			public Model getReference() {
				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
				return referenceMC.getModelBehind();
			}
			
			@Override
			public void createDualCommands(List<CommandState<Model>> commandStates) {
				ModelLocation observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				ModelLocation observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);
				
				commandStates.add(new PendingCommandState<Model>(
					new Model.AddObserverCommand2(observableLocation, observerLocation),
					new Model.RemoveObserverCommand2(observableLocation, observerLocation)
				));
			}
		});
	}
}
