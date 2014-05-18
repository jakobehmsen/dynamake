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
import java.util.Map;

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

	public static final String PROPERTY_COLOR = "Color";
	public static final String PROPERTY_VIEW = "View";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final int VIEW_APPLIANCE = 1;
	protected static final int VIEW_ENGINEERING = 0;
	
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
		
		private String name;
		private Object value;
//		private Object antagonistValue;
		
		public SetPropertyTransaction(String name, Object value/*, Object antagonistValue*/) {
//			if(name.equals("X") && value instanceof Integer)
//				new String();
			
			this.name = name;
			this.value = value;
//			this.antagonistValue = antagonistValue;
		}
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
//			PropogationContext propCtx = new PropogationContext();
			prevalentSystem.setProperty(name, value, propCtx, 0);
		}
//		@Override
//		public Command<Model> antagonist() {
////			return new SetPropertyTransaction(name, antagonistValue, value);
//			return null;
//		}
	}
	
	protected Hashtable<String, Object> properties;
	
	public void setProperty(String name, Object value, PropogationContext propCtx, int propDistance) {
		if(properties == null)
			properties = new Hashtable<String, Object>();
		if(value != null)
			properties.put(name, value);
		else
			properties.remove(name);
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.addObserver(observer);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime) {
			Model observable = (Model)observableLocation.getChild(rootPrevalentSystem);
			Model observer = (Model)observerLocation.getChild(rootPrevalentSystem);
			
			observable.removeObserver(observer);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
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
	
	public static class CompositeTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Command<Model>[] transactions;
		
		public CompositeTransaction(Command<Model>[] transactions) {
			this.transactions = transactions;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
//			PropogationContext propCtx = new PropogationContext();
			prevalentSystem.beginUpdate(propCtx, 0);
			for(Command<Model> t: transactions)
				t.executeOn(propCtx, prevalentSystem, executionTime);
			prevalentSystem.endUpdate(propCtx, 0);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
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

	public void setView(int view, PropogationContext propCtx, int propDistance, int changeDistance) {
		setProperty(Model.PROPERTY_VIEW, view, propCtx, propDistance);
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
						targetComponent.setLocation(new Point(((Number)propertyChanged.value).intValue(), targetComponent.getY()));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Y")) {
						targetComponent.setLocation(new Point(targetComponent.getX(), ((Number)propertyChanged.value).intValue()));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Width")) {
						targetComponent.setSize(new Dimension(((Number)propertyChanged.value).intValue(), targetComponent.getHeight()));
						madeChanges = true;
					} else if(propertyChanged.name.equals("Height")) {
						targetComponent.setSize(new Dimension(targetComponent.getWidth(), ((Number)propertyChanged.value).intValue()));
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
	
	public static final int COMPONENT_COLOR_BACKGROUND = 0;
	public static final int COMPONENT_COLOR_FOREGROUND = 1;
	
	public static RemovableListener wrapForComponentColorChanges(Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager, final int componentColor) {
		return RemovableListener.addObserver(model, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
				if(change instanceof Model.PropertyChanged) {
					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;

					if(propertyChanged.name.equals(PROPERTY_COLOR)) {
						switch(componentColor) {
						case COMPONENT_COLOR_BACKGROUND: {
							targetComponent.setBackground((Color)propertyChanged.value);
							targetComponent.validate();
							viewManager.refresh(view);
						}
						case COMPONENT_COLOR_FOREGROUND: {
							targetComponent.setForeground((Color)propertyChanged.value);
							targetComponent.validate();
							viewManager.repaint(targetComponent);
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime) {
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);
//			PropogationContext propCtx = new PropogationContext(); 
			model.sendChanged(new MouseUp(), propCtx, 0, 0);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
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
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime) {
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);
//			PropogationContext propCtx = new PropogationContext(); 
			model.sendChanged(new MouseDown(), propCtx, 0, 0);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	public static void wrapForComponentGUIEvents(final Model model, final ModelComponent view, final JComponent targetComponent, final ViewManager viewManager) {
		((JComponent)view).addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
//				PropogationContext propCtx = new PropogationContext(); 
//				model.sendChanged(new MouseUp(), propCtx, 0, 0);
				view.getTransactionFactory().executeOnRoot(new PropogationContext(), new MouseUpTransaction(view.getTransactionFactory().getLocation()));
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
//				PropogationContext propCtx = new PropogationContext(); 
//				model.sendChanged(new MouseDown(), propCtx, 0, 0);
				view.getTransactionFactory().executeOnRoot(new PropogationContext(), new MouseDownTransaction(view.getTransactionFactory().getLocation()));
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
		transactions.addTransaction("Set " + PROPERTY_COLOR, new ColorTransactionBuilder((Color)model.getProperty(PROPERTY_COLOR), new Action1<Color>() {
			@Override
			public void run(Color color) {
				transactionFactory.execute(new PropogationContext(), new Model.SetPropertyTransaction(PROPERTY_COLOR, color));
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
			final ModelComponent dropped, final ModelComponent target, final Rectangle droppedBounds, TransactionMapBuilder transactions) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addTransaction("Clone Isolated", new Runnable() {
				@Override
				public void run() {
					Rectangle creationBounds = droppedBounds;

					int addIndex = ((CanvasModel)target.getModelBehind()).getModelCount();
					ModelComponent output = ((LiveModel.LivePanel)livePanel).productionPanel.editPanelMouseAdapter.output;
					Location outputLocation = null;
					if(output != null)
						outputLocation = output.getTransactionFactory().getLocation();
						
					DualCommand<Model> dualCommand = new DualCommandPair<Model>(
						new AddThenOutputTransaction(
							livePanel.getTransactionFactory().getLocation(), 
							target.getTransactionFactory().getLocation(), 
							creationBounds, 
							new CloneIsolatedFactory(dropped.getTransactionFactory().getLocation())
						), 
						new SetOutputThenRemoveAtTransaction(
							((LiveModel.LivePanel)livePanel).getTransactionFactory().getLocation(), 
							outputLocation, 
							target.getTransactionFactory().getLocation(), 
							addIndex
						)
					);
					dropped.getTransactionFactory().executeOnRoot(new PropogationContext(), dualCommand);
//					dropped.getTransactionFactory().executeOnRoot(
//						new PropogationContext(), new AddThenOutputTransaction(
//						livePanel.getTransactionFactory().getLocation(), 
//						target.getTransactionFactory().getLocation(), 
//						creationBounds, 
//						new CloneIsolatedFactory(dropped.getTransactionFactory().getLocation()))
//					);
				}
			});
			transactions.addTransaction("Clone Deep", new Runnable() {
				@Override
				public void run() {
					Rectangle creationBounds = droppedBounds;

					dropped.getTransactionFactory().executeOnRoot(
							new PropogationContext(), new AddThenOutputTransaction(
							livePanel.getTransactionFactory().getLocation(), 
							target.getTransactionFactory().getLocation(), 
							creationBounds, 
							new CloneDeepFactory(dropped.getTransactionFactory().getLocation()))
					);
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
	
	public void scale(Rectangle newBounds, PropogationContext propCtx, int propDistance) {
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		setProperty("X", new Fraction(newBounds.x), propCtx, propDistance);
		setProperty("Y", new Fraction(newBounds.y), propCtx, propDistance);
		setProperty("Width", new Fraction(newBounds.width), propCtx, propDistance);
		setProperty("Height", new Fraction(newBounds.height), propCtx, propDistance);
		
		Fraction hChange = new Fraction(newBounds.width).divide(currentWidth);
		Fraction vChange = new Fraction(newBounds.height).divide(currentHeight);

		modelScale(hChange, vChange, propCtx, propDistance);
	}

	public void scale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance) {
		Fraction currentX = (Fraction)getProperty("X");
		Fraction currentY = (Fraction)getProperty("Y");
		Fraction currentWidth = (Fraction)getProperty("Width");
		Fraction currentHeight = (Fraction)getProperty("Height");
		
		Fraction newX = currentX.multiply(hChange);
		Fraction newY = currentY.multiply(vChange);
		Fraction newWidth = currentWidth.multiply(hChange);
		Fraction newHeight = currentHeight.multiply(vChange);

		setProperty("X", newX, propCtx, propDistance);
		setProperty("Y", newY, propCtx, propDistance);
		setProperty("Width", newWidth, propCtx, propDistance);
		setProperty("Height", newHeight, propCtx, propDistance);
		
		modelScale(hChange, vChange, propCtx, propDistance);
	}
	
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance) {
		
	}
}
