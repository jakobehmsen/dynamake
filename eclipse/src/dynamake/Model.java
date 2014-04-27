package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.prevayler.Transaction;

public abstract class Model implements Serializable, Observer {
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
	
//	public static class MetaModelLocator implements Locator {
//		@Override
//		public Location locate() {
//			return new MetaModelLocation();
//		}
//	}
	
//	public static class MetaModelLocation implements Location {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//
//		@Override
//		public Object getChild(Object holder) {
//			return ((Model)holder).getMetaModel();
//		}
//		
//		@Override
//		public void setChild(Object holder, Object child) {
//			// TODO Auto-generated method stub
//			
//		}
//	}
//	
//	private Map metaModel;
	
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
	
//	public Map getMetaModel() {
//		if(metaModel == null) {
//			metaModel = new Map();
//			// Default values here?
//		}
//		return metaModel;
//	}
	
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
	
	private transient ArrayList<Observer> observers;
	private transient ArrayList<Observer> observersToAdd;
	private transient ArrayList<Observer> observersToRemove;
	
	public Model() {
		observers = new ArrayList<Observer>();
	}
	
	private void writeObject(ObjectOutputStream ous) throws IOException {
		ArrayList<Observer> observersToSerialize = new ArrayList<Observer>();
		for(Observer o: observers) {
			if(o instanceof Model)
				observersToSerialize.add(o);
		}
		ous.writeObject(observersToSerialize);
		ous.writeObject(properties);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//		observers = new ArrayList<Observer>();
		observers = (ArrayList<Observer>)ois.readObject();
		properties = (Hashtable<String, Object>)ois.readObject();
	}
	
	protected void sendChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		int nextChangeDistance = changeDistance + 1;
		int nextPropDistance = propDistance + 1;
		observersToAdd = new ArrayList<Observer>();
		observersToRemove = new ArrayList<Observer>();
		for(Observer observer: observers)
			observer.changed(this, change, propCtx, nextPropDistance, nextChangeDistance);
		if(observersToAdd == null) {
			new String();
		}
		for(Observer observerToAdd: observersToAdd)
			observers.add(observerToAdd);
		for(Observer observerToRemove: observersToRemove)
			observers.remove(observerToRemove);
		observersToAdd = null;
		observersToRemove = null;
	}
	
	public void addObserver(Observer observer) {
		if(observersToAdd == null)
			observers.add(observer);
		else
			observersToAdd.add(observer);
	}
	
	public void removeObserver(Observer observer) {
		if(observersToRemove == null)
			observers.remove(observer);
		else
			observersToRemove.add(observer);
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
		return RemovableListener.addObserver(model, new Observer() {
			boolean isUpdating;
			boolean madeChanges;
			
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				model.toString();
				if(change instanceof Model.PropertyChanged) {
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
		return RemovableListener.addObserver(model, new Observer() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof Model.PropertyChanged) {
					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
					if(propertyChanged.name.equals("Background")) {
						targetComponent.setBackground((Color)propertyChanged.value);
						targetComponent.validate();
						viewManager.refresh(view);
					} else if(propertyChanged.name.equals("Foreground")) {
						targetComponent.setForeground((Color)propertyChanged.value);
						targetComponent.validate();
						viewManager.repaint(targetComponent);
					}
				}
			}
		});
	}
	
	public static void loadComponentProperties(Model model, Component view) {
		Object background = model.getProperty("Background");
		if(background != null)
			view.setBackground((Color)background);
		
		Object foreground = model.getProperty("Foreground");
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
		return Model.RemovableListener.addObserver(model, new Observer() {
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
//		final TransactionFactory metaTransactionFactory = transactionFactory.extend(new MetaModelLocator());
		
		transactions.addTransaction("Set Background", new ColorTransactionBuilder((Color)model.getProperty("Background"), new Action1<Color>() {
			@Override
			public void run(Color color) {
				transactionFactory.execute(new Model.SetPropertyTransaction("Background", color));
			}
		}));
		
		transactions.addTransaction("Set Foreground", new ColorTransactionBuilder((Color)model.getProperty("Foreground"), new Action1<Color>() {
			@Override
			public void run(Color color) {
				transactionFactory.execute(new Model.SetPropertyTransaction("Foreground", color));
			}
		}));
	}
}
