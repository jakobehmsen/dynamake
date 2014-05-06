package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.prevayler.Transaction;

public abstract class Model implements Serializable, Observer {
	public static class TellProperty {
		public final String name;

		public TellProperty(String name) {
			this.name = name;
		}
	}

	public static class MouseDown {

	}
	
	public static class MouseUp {

	}

	public static final String PROPERTY_BACKGROUND = "Background";
	public static final String PROPERTY_FOREGROUND = "Foreground";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
	
	public static class SetPropertyTransaction implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String name;
		private Object value;
		
		public SetPropertyTransaction(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		@Override
		public void executeOn(Model prevalentSystem, Date executionTime) {
			PropogationContext propCtx = new PropogationContext();
			prevalentSystem.setProperty(name, value, propCtx, 0);
		}
	}
	
	private Hashtable<String, Object> properties;
	
	public void setProperty(String name, Object value, PropogationContext propCtx, int propDistance) {
		if(properties == null)
			properties = new Hashtable<String, Object>();
		properties.put(name, value);
		sendChanged(new PropertyChanged(name, value), propCtx, propDistance, 0);
	}
	
	public Object getProperty(String name) {
		if(properties != null)
			return properties.get(name);
		return null;
	}
	
	public static class BeganUpdate {
		
	}
	
	public static class EndedUpdate {
		
	}
	
	public static class AddObserver implements Transaction<Model> {
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
		public void executeOn(Model rootPrevalentSystem, Date executionTime) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
		}
	}
	
	public static class RemoveObserver implements Transaction<Model> {
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
		public void executeOn(Model rootPrevalentSystem, Date executionTime) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
		}
	}
	
	public void beginUpdate(PropogationContext propCtx, int propDistance) {
		sendChanged(new BeganUpdate(), propCtx, propDistance, 0);
	}
	
	public void endUpdate(PropogationContext propCtx, int propDistance) {
		sendChanged(new EndedUpdate(), propCtx, propDistance, 0);
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		if(change instanceof SetProperty && changeDistance == 1) {
			SetProperty setProperty = (SetProperty)change;
			setProperty(setProperty.name, setProperty.value, propCtx, propDistance);
		} else if(change instanceof TellProperty && changeDistance == 1) {
			TellProperty tellProperty = (TellProperty)change;
			Object value = getProperty(tellProperty.name);
			if(value != null)
				sendChanged(new Model.PropertyChanged(tellProperty.name, value), propCtx, propDistance, 0);
		} else {
			modelChanged(sender, change, propCtx, propDistance, changeDistance);
		}
	}
	
	protected void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		
	}
	
	public static class CompositeTransaction implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Transaction<Model>[] transactions;
		
		public CompositeTransaction(Transaction<Model>[] transactions) {
			this.transactions = transactions;
		}

		@Override
		public void executeOn(Model prevalentSystem, Date executionTime) {
			PropogationContext propCtx = new PropogationContext();
			prevalentSystem.beginUpdate(propCtx, 0);
			for(Transaction<Model> t: transactions)
				t.executeOn(prevalentSystem, executionTime);
			prevalentSystem.endUpdate(propCtx, 0);
		}
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

	public abstract Binding<ModelComponent> createView(ViewManager viewManager, TransactionFactory transactionFactory);
	
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
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		observers = (ArrayList<Observer>)ois.readObject();
		observees = (ArrayList<Observer>)ois.readObject();
		properties = (Hashtable<String, Object>)ois.readObject();
	}
	
	protected void sendChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		if(changeQueue != null) {
			changeQueue.add(new ChangeHolder(change, propCtx, propDistance, changeDistance));
			return;
		}
		
		changeQueue = new ArrayDeque<Model.ChangeHolder>();
		sendSingleChanged(change, propCtx, propDistance, changeDistance);
		while(true) {
			ChangeHolder changeHolder = changeQueue.poll();
			if(changeHolder == null)
				break;
			sendSingleChanged(changeHolder.change, changeHolder.propCtx, changeHolder.propDistance, changeHolder.changeDistance);
		}
		changeQueue = null;
		
//		int nextChangeDistance = changeDistance + 1;
//		int nextPropDistance = propDistance + 1;
//		observersToAdd = new ArrayList<Observer>();
//		observersToRemove = new ArrayList<Observer>();
//		for(Observer observer: observers)
//			observer.changed(this, change, propCtx, nextPropDistance, nextChangeDistance);
//		for(Observer observerToAdd: observersToAdd) {
//			observers.add(observerToAdd);
//			observerToAdd.addObservee(this);
//		}
//		for(Observer observerToRemove: observersToRemove) {
//			observers.remove(observerToRemove);
//			observerToRemove.removeObservee(this);
//		}
//		observersToAdd = null;
//		observersToRemove = null;
	}
	
	protected void sendSingleChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		int nextChangeDistance = changeDistance + 1;
		int nextPropDistance = propDistance + 1;
		observersToAdd = new ArrayList<Observer>();
		observersToRemove = new ArrayList<Observer>();
		for(Observer observer: observers)
			observer.changed(this, change, propCtx, nextPropDistance, nextChangeDistance);
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
	
	public JFrame toFrame(ViewManager viewManager, TransactionFactory transactionFactory) {
		JFrame frame = new JFrame();
		
		frame.getContentPane().setLayout(new BorderLayout());
		final Binding<ModelComponent> view = createView(viewManager, transactionFactory);
		frame.getContentPane().add((JComponent)view.getBindingTarget(), BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				view.releaseBinding();
			}
		});
		
		return frame;
	}
	
	public static RemovableListener wrapForBoundsChanges(final Model model, final ModelComponent target, final ViewManager viewManager) {
		return RemovableListener.addObserver(model, new ObserverAdapter() {
			boolean isUpdating;
			boolean madeChanges;
			
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof Model.PropertyChanged
						&& changeDistance == 1 /* And not a forwarded change */) {
					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					Component targetComponent = ((Component)target);
					if(propertyChanged.name.equals("X")) {
						targetComponent.setLocation(new Point((int)propertyChanged.value, targetComponent.getY()));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Y")) {
						targetComponent.setLocation(new Point(targetComponent.getX(), (int)propertyChanged.value));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Width")) {
						targetComponent.setSize(new Dimension((int)propertyChanged.value, targetComponent.getHeight()));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Height")) {
						targetComponent.setSize(new Dimension(targetComponent.getWidth(), (int)propertyChanged.value));
						madeChanges = true;
					}
				} else if(change instanceof BeganUpdate) {
					isUpdating = true;
				} else if(change instanceof EndedUpdate) {
					isUpdating = false;
					
					if(madeChanges) {
						((JComponent)target).validate();
						viewManager.refresh(target);
						madeChanges = false;
					}
				}
				
				if(!isUpdating) {
					if(madeChanges) {
						viewManager.refresh(target);
						madeChanges = false;
					}
				}
			}
		});
	}
	
	public static RemovableListener wrapForComponentPropertyChanges(Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager) {
		return RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof Model.PropertyChanged) {
					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals(PROPERTY_BACKGROUND)) {
						targetComponent.setBackground((Color)propertyChanged.value);
						targetComponent.validate();
						viewManager.refresh(view);
					} else if(propertyChanged.name.equals(PROPERTY_FOREGROUND)) {
						targetComponent.setForeground((Color)propertyChanged.value);
						targetComponent.validate();
						viewManager.repaint(targetComponent);
					}
				}
			}
		});
	}
	
	public static void wrapForComponentGUIEvents(final Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager) {
		((JComponent)view).addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				PropogationContext propCtx = new PropogationContext(); 
				model.sendChanged(new MouseUp(), propCtx, 0, 0);
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				PropogationContext propCtx = new PropogationContext(); 
				model.sendChanged(new MouseDown(), propCtx, 0, 0);
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	public static void loadComponentProperties(Model model, Component view) {
		Object background = model.getProperty(PROPERTY_BACKGROUND);
		if(background != null)
			view.setBackground((Color)background);
		
		Object foreground = model.getProperty(PROPERTY_FOREGROUND);
		if(foreground != null)
			view.setForeground((Color)foreground);
	}
	
	public static void loadComponentBounds(Model model, Component view) {
		Integer x = (Integer)model.getProperty("X");
		if(x != null)
			view.setLocation(x, view.getY());
		
		Integer y = (Integer)model.getProperty("Y");
		if(y != null)
			view.setLocation(view.getX(), y);
		
		Integer width = (Integer)model.getProperty("Width");
		if(width != null)
			view.setSize(width, view.getHeight());
		
		Integer height = (Integer)model.getProperty("Height");
		if(height != null)
			view.setSize(view.getWidth(), height);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Model.RemovableListener bindProperty(Model model, final String modelPropertyName, final Action1<T> propertySetter) {
		Object value = model.getProperty(modelPropertyName);
		if(value != null)
			propertySetter.run((T)value);
		return Model.RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof Model.PropertyChanged 
					&& changeDistance == 1 /* And not a forwarded change */) {
					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals(modelPropertyName))
						propertySetter.run((T)propertyChanged.value);
				}
			}
		});
	}
	
	public static void appendComponentPropertyChangeTransactions(final Model model, final TransactionFactory transactionFactory, TransactionMapBuilder transactions) {
		transactions.addTransaction("Set " + PROPERTY_BACKGROUND, new ColorTransactionBuilder((Color)model.getProperty(PROPERTY_BACKGROUND), new Action1<Color>() {
			@Override
			public void run(Color color) {
				transactionFactory.execute(new Model.SetPropertyTransaction(PROPERTY_BACKGROUND, color));
			}
		}));
		
		transactions.addTransaction("Set " + PROPERTY_FOREGROUND, new ColorTransactionBuilder((Color)model.getProperty(PROPERTY_FOREGROUND), new Action1<Color>() {
			@Override
			public void run(Color color) {
				transactionFactory.execute(new Model.SetPropertyTransaction(PROPERTY_FOREGROUND, color));
			}
		}));
	}

	public void beRemoved() {
		for(Observer observer: new ArrayList<Observer>(observers)) {
			observer.removeObservee(this);
		}
		for(Observer observee: new ArrayList<Observer>(observees)) {
			if(observee instanceof Model)
				((Model)observee).removeObserver(this);
		}
	}

	public static void appendGeneralDroppedTransactions(final ModelComponent dropped,
			final ModelComponent target, final Rectangle droppedBounds, TransactionMapBuilder transactions) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addTransaction("Clone Isolated", new Runnable() {
				@Override
				public void run() {
					Rectangle creationBounds = droppedBounds;

					dropped.getTransactionFactory().executeOnRoot(
						new CanvasModel.AddModelTransaction(
							target.getTransactionFactory().getLocation(), 
							creationBounds, 
							new CloneIsolatedFactory(dropped.getTransactionFactory().getLocation()))
					);
				}
			});
			transactions.addTransaction("Clone Deep", new Runnable() {
				@Override
				public void run() {
					Rectangle creationBounds = droppedBounds;

					dropped.getTransactionFactory().executeOnRoot(
						new CanvasModel.AddModelTransaction(
							target.getTransactionFactory().getLocation(), 
							creationBounds, 
							new CloneDeepFactory(dropped.getTransactionFactory().getLocation()))
					);
				}
			});
		}
	}

	public abstract Model modelCloneIsolated();

	protected Model modelCloneDeep(Hashtable<Model, Model> visited) {
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
		return cloneDeep(new Hashtable<Model, Model>());
	}
	
	protected Model cloneDeep(Hashtable<Model, Model> visited) {
		/*
		TODO: Fix the below issue:
		There is an issue with the current implementation, because the obervers and obervees, which aren't contained by other models, 
		are cloned, and thus will not be visually represented. This must not occur: a model alive must be visually represented in some 
		manner - or explicitly hidden (not implemented yet).
		*/
		Model clone = modelCloneDeep(visited);
		
		if(clone.properties == null)
			clone.properties = new Hashtable<String, Object>();
		// Assumed that cloning is not necessary for properties
		// I.e., all property values are immutable
		clone.properties.putAll(this.properties);
		
		visited.put(this, clone);
		
		for(Observer observer: this.observers) {
			if(observer instanceof Model) {
				Model observerClone = visited.get(observer);
				if(observerClone == null) {
					observerClone = ((Model)observer).cloneDeep(visited);
				}
				clone.observers.add(observerClone);
				observerClone.observees.add(clone);
			}
		}
		
		for(Observer observee: this.observees) {
			if(observee instanceof Model) {
				Model observeeClone = visited.get(observee);
				if(observeeClone == null) {
					observeeClone = ((Model)observee).cloneDeep(visited);
				}
				observeeClone.observers.add(clone);
				clone.observees.add(observeeClone);
			}
		}
		
		return clone;
	}

	public void inject(Model model) {
		for(Observer observer: this.observers) {
			if(observer instanceof Model) {
				model.observers.add(observer);
				((Model)observer).observees.add(model);
			}
		}
		for(Observer observee: this.observees) {
			if(observee instanceof Model) {
				((Model)observee).addObserver(model);
				model.observees.add(observee);
			}
		}
	}
}
