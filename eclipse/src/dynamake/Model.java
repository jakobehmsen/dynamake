package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.prevayler.Transaction;

public abstract class Model implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static class PropertyChanged {
		public final String name;
		public final Object value;

		public PropertyChanged(String name, Object value) {
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
			prevalentSystem.setProperty(name, value);
		}
	}
	
	private Hashtable<String, Object> properties = new Hashtable<String, Object>();

	public interface Observer {
		void changed(Model sender, Object change);
	}
	
	public void setProperty(String name, Object value) {
		properties.put(name, value);
		sendChanged(new PropertyChanged(name, value));
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
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
	}
	
//	public abstract void appendTransactions(JComponent view, TransactionMapBuilder transactions);
//	
	public abstract Binding<ModelComponent> createView(ViewManager viewManager, TransactionFactory transactionFactory);
	
	private transient ArrayList<Model.Observer> observers;
	private transient ArrayList<Model.Observer> observersToAdd;
	private transient ArrayList<Model.Observer> observersToRemove;
	
	public Model() {
		observers = new ArrayList<Model.Observer>();
	}
	
//	private void writeObject(ObjectOutputStream oos)
//			throws IOException {
//			    // default serialization 
//			    oos.defaultWriteObject();
//			    // write the object
//			    List loc = new ArrayList();
//			    loc.add(location.x);
//			    loc.add(location.y);
//			    loc.add(location.z);
//			    loc.add(location.uid);
//			    oos.writeObject(loc);
//			}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//	    // default deserialization
//	    ois.defaultReadObject();
//	    List loc = (List)ois.readObject(); // Replace with real deserialization
//	    location = new Location(loc.get(0), loc.get(1), loc.get(2), loc.get(3));
//	    // ... more code
		observers = new ArrayList<Model.Observer>();
		properties = (Hashtable<String, Object>)ois.readObject();
	}
	
	protected void sendChanged(Object change) {
		observersToAdd = new ArrayList<Model.Observer>();
		observersToRemove = new ArrayList<Model.Observer>();
		for(Model.Observer observer: observers)
			observer.changed(this, change);
		for(Model.Observer observerToAdd: observersToAdd)
			observers.add(observerToAdd);
		for(Model.Observer observerToRemove: observersToRemove)
			observers.remove(observerToRemove);
		observersToAdd = null;
		observersToRemove = null;
	}
	
	public void addObserver(Model.Observer observer) {
		if(observersToAdd == null)
			observers.add(observer);
		else
			observersToAdd.add(observer);
	}
	
	public void removeObserver(Model.Observer observer) {
		if(observersToRemove == null)
			observers.remove(observer);
		else
			observersToRemove.add(observer);
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

	public static void wrapForFocus(final ViewManager viewManager, final JComponent container, JComponent target) {
		wrapForFocus(viewManager, container, target, new Func0<Boolean>() {
			@Override
			public Boolean call() {
				return true;
			}
		});
	}

	public static void wrapForFocus(final ViewManager viewManager, final JComponent container, JComponent target, final Func0<Boolean> focusCondition) {
//		target.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseEntered(MouseEvent e) {
//				if(focusCondition.call())
//					viewManager.setFocus(container);
//			}
//			
//			@Override
//			public void mouseExited(MouseEvent e) { }
//		});
	}
	
	public static RemovableListener wrapForBoundsChanges(Model model, final Component target) {
		return RemovableListener.addObserver(model, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals("X")) {
						target.setLocation(new Point((int)propertyChanged.value, target.getY()));
						target.validate();
					} else if(propertyChanged.name.equals("Y")) {
						target.setLocation(new Point(target.getX(), (int)propertyChanged.value));
						target.validate();
					} else if(propertyChanged.name.equals("Width")) {
						target.setSize(new Dimension((int)propertyChanged.value, target.getHeight()));
						target.validate();
					} else if(propertyChanged.name.equals("Height")) {
						target.setSize(new Dimension(target.getWidth(), (int)propertyChanged.value));
						target.validate();
					}
				}
			}
		});
	}
	
	public static RemovableListener wrapForComponentPropertyChanges(Model model, final Component target) {
		return RemovableListener.addObserver(model, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals("Background")) {
						target.setBackground((Color)propertyChanged.value);
						target.revalidate();
						target.repaint();
					} else if(propertyChanged.name.equals("Foreground")) {
						target.setForeground((Color)propertyChanged.value);
						target.invalidate();
						target.repaint();
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
	
	public static void appendComponentPropertyChangeTransactions(final Model model, final TransactionFactory transactionFactory, TransactionMapBuilder transactions) {
		LinkedHashMap<String, Color> colors = new LinkedHashMap<String, Color>();
		colors.put("Black", Color.BLACK);
		colors.put("Blue", Color.BLUE);
		colors.put("Cyan", Color.CYAN);
		colors.put("Dark Gray", Color.DARK_GRAY);
		colors.put("Gray", Color.GRAY);
		colors.put("Green", Color.GREEN);
		colors.put("Light Gray", Color.LIGHT_GRAY);
		colors.put("Magenta", Color.MAGENTA);
		colors.put("Orange", Color.ORANGE);
		colors.put("Pink", Color.PINK);
		colors.put("Red", Color.RED);
		colors.put("White", Color.WHITE);
		colors.put("Yellow", Color.YELLOW);
		
		TransactionMapBuilder backgroundMapBuilder = new TransactionMapBuilder(); 
		
		for(final Map.Entry<String, Color> entry: colors.entrySet()) {
			backgroundMapBuilder.addTransaction(entry.getKey(), new Runnable() {
				@Override
				public void run() {
					transactionFactory.execute(new Model.SetPropertyTransaction("Background", entry.getValue()));
				}
			});
		}
		
		TransactionMapBuilder foregroundMapBuilder = new TransactionMapBuilder(); 
		
		for(final Map.Entry<String, Color> entry: colors.entrySet()) {
			foregroundMapBuilder.addTransaction(entry.getKey(), new Runnable() {
				@Override
				public void run() {
					transactionFactory.execute(new Model.SetPropertyTransaction("Foreground", entry.getValue()));
				}
			});
		}
		
//		transactions.addTransaction("Set Background", backgroundMapBuilder);
//		transactions.addTransaction("Set Foreground", foregroundMapBuilder);
		
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
