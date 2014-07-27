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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JComponent;

import dynamake.commands.AddObserverCommand;
import dynamake.commands.Command;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateSequence;
import dynamake.commands.PendingCommandFactory;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RemoveObserverCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.RevertingCommandStateSequence;
import dynamake.commands.SetPropertyCommand;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.menubuilders.ColorMenuBuilder;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.factories.CloneDeepFactory;
import dynamake.models.factories.CloneIsolatedFactory;
import dynamake.models.factories.ModelFactory;
import dynamake.models.factories.NewInstanceFactory;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.Trigger;

/**
 * Instances of implementors are supposed to represent alive-like sensitive entities, each with its own local history.
 */
public abstract class Model implements Serializable, Observer {
	public static class PendingUndoablePair implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final PendingCommandState<Model> pending;
		public final ReversibleCommand<Model> undoable;
		
		public PendingUndoablePair(PendingCommandState<Model> pending, ReversibleCommand<Model> undoable) {
			this.pending = pending;
			this.undoable = undoable;
		}
	}
	
	public static class UndoRedoPart implements Serializable, CommandState<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final CommandState<Model> origin;
		public final CommandState<Model> revertible;
		
		public UndoRedoPart(CommandState<Model> origin, CommandState<Model> revertible) {
			this.origin = origin;
			this.revertible = revertible;
		}

		@Override
		public CommandState<Model> executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			CommandState<Model> reverted = revertible.executeOn(propCtx, prevalentSystem, collector, location);
			return new UndoRedoPart(origin, reverted);
		}
	}
	
	public static class DualCommand implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final CommandState<Model> forward;
		public final CommandState<Model> backward;
		
		public DualCommand(CommandState<Model> forward, CommandState<Model> backward) {
			this.forward = forward;
			this.backward = backward;
		}
	}
	
	public static class HistoryAppendLogChange {
		public final List<PendingUndoablePair> pendingUndoablePairs;
		
		public HistoryAppendLogChange(List<PendingUndoablePair> pendingCommands) {
			this.pendingUndoablePairs = pendingCommands;
		}
	}
	
	public static class HistoryAppendLogChange2 {
		public final List<PendingUndoablePair> pendingUndoablePairs;
		
		public HistoryAppendLogChange2(List<PendingUndoablePair> pendingCommands) {
			this.pendingUndoablePairs = pendingCommands;
		}
	}
	
	public static class HistoryChange {
		public static final int TYPE_UNDO = 0;
		public static final int TYPE_REDO = 1;
		
		public final int type;
		
		public HistoryChange(int type) {
			this.type = type;
		}
	}
	
	public static class HistoryLogChange {
		public static final int TYPE_COMMIT_LOG = 0;
		public static final int TYPE_REJECT_LOG = 1;
		
		public final int type;
		public final int length;
		public final ArrayList<Model.PendingUndoablePair> newLog;
		
		public HistoryLogChange(int type, int length, ArrayList<Model.PendingUndoablePair> newLog) {
			this.type = type;
			this.length = length;
			this.newLog = newLog;
		}
	}
	
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

	private ArrayList<PendingUndoablePair> newLog = new ArrayList<Model.PendingUndoablePair>();

	public void appendLog(ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println("Log");
		
		newLog.addAll(pendingUndoablePairs);
		redoStack2.clear();

		sendChanged(new HistoryAppendLogChange(pendingUndoablePairs), propCtx, propDistance, 0, collector);
	}
	
	public void postLog(ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println("Log");
		
		sendChanged(new HistoryAppendLogChange(pendingUndoablePairs), propCtx, propDistance, 0, collector);
	}
	
	public void startLog() {
		
	}
	
	public void commitLog(int length, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(newLog.size() > 0) {
			@SuppressWarnings("unchecked")
			CommandState<Model>[] compressedLogPartAsArray = (CommandState<Model>[])new CommandState[length];
//			ArrayList<UndoRedoPart> compressedLogPart = new ArrayList<Model.UndoRedoPart>();
			for(int i = 0; i < length; i++) {
				compressedLogPartAsArray[i] = new UndoRedoPart(newLog.get(i).pending, newLog.get(i).undoable);
//				compressedLogPart.add(new UndoRedoPart(newLog.get(i), newLog.get(i).undoable));
			}
			RevertingCommandStateSequence<Model> compressedLogPart = RevertingCommandStateSequence.reverse(compressedLogPartAsArray);
			undoStack2.add(compressedLogPart);
		}
		@SuppressWarnings("unchecked")
		ArrayList<PendingUndoablePair> newLogCopy = (ArrayList<PendingUndoablePair>)newLog.clone();
		newLog.clear();
		
		sendChanged(new HistoryLogChange(HistoryLogChange.TYPE_COMMIT_LOG, length, newLogCopy), propCtx, propDistance, 0, collector);
	}
	
	public void rejectLog(int length, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		newLog.clear();
		
		sendChanged(new HistoryLogChange(HistoryLogChange.TYPE_REJECT_LOG, length, null), propCtx, propDistance, 0, collector);
	}
	
	public void unplay2(int count, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		redoStack2.clear();
		
//		while(!undoStack2.isEmpty()) {
		for(int i = 0; i < count; i++) {
//			ArrayList<UndoRedoPart> redoablePairs = new ArrayList<UndoRedoPart>();
//			List<UndoRedoPart> pairsToUndo = undoStack2.pop();
			CommandState<Model> toUndo = undoStack2.pop();
			CommandState<Model> redoable = toUndo.executeOn(propCtx, this, collector, new ModelRootLocation());
			redoStack2.push(redoable);
			
//			for(UndoRedoPart pair: pairsToUndo) {
//				CommandState<Model> redoable = pair.revertible.executeOn(propCtx, this, collector, new ModelRootLocation());
//				redoablePairs.add(new UndoRedoPart(pair.origin, (ReversibleCommand<Model>)redoable));
//			}
//			
//			redoStack2.push(redoablePairs);
		}
	}	
	
	public void replay2(int count, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		while(!redoStack2.isEmpty()) {
		for(int i = 0; i < count; i++) {
			CommandState<Model> toRedo = redoStack2.pop();
			CommandState<Model> undoable = toRedo.executeOn(propCtx, this, collector, new ModelRootLocation());
			undoStack2.push(undoable);
			
//			ArrayList<UndoRedoPart> undoablePairs = new ArrayList<UndoRedoPart>();
//			List<UndoRedoPart> pairsToRedo = redoStack2.pop();
//			
//			for(UndoRedoPart pair: pairsToRedo) {
//				CommandState<Model> undoable = pair.revertible.executeOn(propCtx, this, collector, new ModelRootLocation());
//				undoablePairs.add(new UndoRedoPart(pair.origin, (ReversibleCommand<Model>)undoable));
//			}
//			
//			undoStack2.push(undoablePairs);
		}
	}
	
	public CommandState<Model> undo2(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(!undoStack2.isEmpty()) {
			CommandState<Model> toUndo = undoStack2.pop();
			CommandState<Model> redoable = toUndo.executeOn(propCtx, this, collector, new ModelRootLocation());
			redoStack2.push(redoable);
			
			return toUndo;
			
//			ArrayList<UndoRedoPart> redoablePairs = new ArrayList<Model.UndoRedoPart>();
//			List<UndoRedoPart> pairsToUndo = undoStack2.pop();
//			
//			ArrayList<CommandState<Model>> forwards = new ArrayList<CommandState<Model>>();
//			ArrayList<CommandState<Model>> backwards = new ArrayList<CommandState<Model>>(); 
//			
//			for(UndoRedoPart pair: pairsToUndo) {
//				CommandState<Model> redoable = pair.revertible.executeOn(propCtx, this, collector, new ModelRootLocation());
//				redoablePairs.add(new UndoRedoPart(pair.origin, (ReversibleCommand<Model>)redoable));
//				
//				forwards.add(pair.revertible);
//				backwards.add(redoable);
//			}
//			
//			redoStack2.push(redoablePairs);
//
//			return new DualCommand(
//				new CommandStateSequence<Model>(forwards),
//				new CommandStateSequence<Model>(backwards)
//			);
		}
		
		return null;
	}	
	
	public CommandState<Model> redo2(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(!redoStack2.isEmpty()) {
			CommandState<Model> toRedo = redoStack2.pop();
			CommandState<Model> undoable = toRedo.executeOn(propCtx, this, collector, new ModelRootLocation());
			undoStack2.push(undoable);
			
			return toRedo;
			
//			ArrayList<UndoRedoPart> undoablePairs = new ArrayList<UndoRedoPart>();
//			List<UndoRedoPart> pairsToRedo = redoStack2.pop();
//			
//			ArrayList<CommandState<Model>> forwards = new ArrayList<CommandState<Model>>();
//			ArrayList<CommandState<Model>> backwards = new ArrayList<CommandState<Model>>(); 
//			
//			for(UndoRedoPart pair: pairsToRedo) {
//				// undoable is used to revert using output from the previous command execution
//				CommandState<Model> undoable = pair.revertible.executeOn(propCtx, this, collector, new ModelRootLocation());
//				undoablePairs.add(new UndoRedoPart(pair.origin, (ReversibleCommand<Model>)undoable));
//				
//				forwards.add(pair.revertible);
//				backwards.add(undoable);
//			}
//			
//			undoStack2.push(undoablePairs);
//
//			return new DualCommand(
//				new CommandStateSequence<Model>(forwards),
//				new CommandStateSequence<Model>(backwards)
//			);
		}
		
		return null;
	}

	public List<CommandState<Model>> play(List<CommandState<Model>> toPlay, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		ArrayList<CommandState<Model>> newCommandStates = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> cs: toPlay) {
			CommandState<Model> newCS = cs.executeOn(propCtx, this, collector, new ModelRootLocation()); 
			newCommandStates.add(newCS);
		}
		
		return newCommandStates;
	}
	
	public void playForwards2(List<DualCommand> commandStates, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(DualCommand commandStateList: commandStates) {
			@SuppressWarnings("unused")
			CommandState<Model> undoable = commandStateList.forward.executeOn(propCtx, this, collector, new ModelRootLocation());
		}
	}
	
	public void playBackwards2(List<DualCommand> commandStates, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(int i = commandStates.size() - 1; i >= 0; i--) {
			DualCommand commandStateList = commandStates.get(i);
			@SuppressWarnings("unused")
			CommandState<Model> undoable = commandStateList.forward.executeOn(propCtx, this, collector, new ModelRootLocation());
		}
	}

	public boolean canUndo() {
		return undoStack2.size() > 0;
	}

	public boolean canRedo() {
		return redoStack2.size() > 0;
	}
	
	public CommandState<Model> getUnplayable() {
		return RevertingCommandStateSequence.reverse(undoStack2);
	}

	public List<CommandState<Model>> getLocalChanges() {
		ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> undoable: undoStack2) {
			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
			for(int i = 0; i < undoableAsRevertiable.getCommandStateCount(); i++) {
				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
				origins.add(undoPart.origin);
			}
		}
		
		return origins;
		
//		for(List<UndoRedoPart> pendingUndoablePairList: undoStack2) {
//			for(UndoRedoPart pendingUndoablePair: pendingUndoablePairList) {
//				localChanges.add(new DualCommand(pendingUndoablePair.origin.pending, pendingUndoablePair.origin.undoable));
//			}
//		}
//		
//		return localChanges;
	}

	public List<CommandState<Model>> getLocalChangesBackwards() {
		ArrayList<CommandState<Model>> backwards = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> undoable: undoStack2) {
			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
			for(int i = 0; i < undoableAsRevertiable.getCommandStateCount(); i++) {
				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
				backwards.add(undoPart.revertible);
			}
		}
		
		return backwards;
		
//		for(List<UndoRedoPart> pendingUndoablePairList: undoStack2) {
//			for(UndoRedoPart pendingUndoablePair: pendingUndoablePairList) {
//				localChanges.add(new DualCommand(pendingUndoablePair.origin.pending, pendingUndoablePair.origin.undoable));
//			}
//		}
//		
//		return localChanges;
	}

	public int getLocalChangeCount() {
		return undoStack2.size();
	}
	
	public void setLocator(Locator locator) {
		this.locator = locator;
	}
	
	public Locator getLocator() {
		return locator;
	}
	
	public void setParent(Model parent) {
		this.parent = parent;
//		System.out.println("Set parent of " + this + " to " + parent);
	}
	
	public Model getParent() {
		return parent;
	}
	
	/* Both undo- and stack are assumed to contain RevertingCommandStateSequence<Model> objects */
	protected Stack<CommandState<Model>> undoStack2 = new Stack<CommandState<Model>>();
	protected Stack<CommandState<Model>> redoStack2 = new Stack<CommandState<Model>>();
	private Locator locator;
	private Model parent;
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

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof SetProperty && changeDistance == 1) {
			// Side-effect
			final SetProperty setProperty = (SetProperty)change;

			collector.execute(new PendingCommandFactory<Model>() {
				@Override
				public Model getReference() {
					return Model.this;
				}
				
				@Override
				public void createPendingCommand(List<CommandState<Model>> commandStates) {
					commandStates.add(new PendingCommandState<Model>(
						new SetPropertyCommand(setProperty.name, setProperty.value),
						new SetPropertyCommand.AfterSetProperty()
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
			if(o instanceof Serializable)
				observersToSerialize.add(o);
		}
		ous.writeObject(observersToSerialize);
		ArrayList<Observer> observeesToSerialize = new ArrayList<Observer>();
		for(Observer o: observees) {
			if(o instanceof Serializable)
				observeesToSerialize.add(o);
		}
		ous.writeObject(observeesToSerialize);
		ous.writeObject(properties);
		ous.writeObject(undoStack2);
		ous.writeObject(redoStack2);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		observers = (ArrayList<Observer>)ois.readObject();
		observees = (ArrayList<Observer>)ois.readObject();
		properties = (Hashtable<String, Object>)ois.readObject();
		undoStack2 = (Stack<CommandState<Model>>)ois.readObject();
		redoStack2 = (Stack<CommandState<Model>>)ois.readObject();
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
	
	public void removeObserverLike(Observer observer) {
		observers.remove(observer);
		observer.removeObservee(this);
	}

	@SuppressWarnings("unchecked")
	public <T extends Observer> T getObserverOfLike(T observer) {
		int indexOfObserver = observers.indexOf(observer);
		return (T)observers.get(indexOfObserver);
	}

	@SuppressWarnings("unchecked")
	public <T extends Observer> T getObserverOfType(Class<T> c) {
		for(Observer observer: observers) {
			if(c.isInstance(observer))
				return (T)observer;
		}
		
		return null;
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
//									System.out.println("Change background");
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
	
	private static class MouseUpCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location location) {
			Model model = (Model)location.getChild(rootPrevalentSystem);
			model.sendChanged(new MouseUp(), propCtx, 0, 0, collector);
			
			return null;
		}
	}
	
	private static class MouseDownCommand implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Object executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Collector<Model> collector, Location location) {
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

				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.execute(new PendingCommandFactory<Model>() {
							@Override
							public Model getReference() {
								return view.getModelBehind();
							}

							@Override
							public void createPendingCommand(List<CommandState<Model>> commandStates) {
								commandStates.add(new PendingCommandState<Model>(
									new MouseUpCommand(), 
									new Command.Null<Model>()
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

				connection.trigger(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						collector.execute(new PendingCommandFactory<Model>() {
							@Override
							public Model getReference() {
								return view.getModelBehind();
							}

							@Override
							public void createPendingCommand(List<CommandState<Model>> commandStates) {
								commandStates.add(new PendingCommandState<Model>(
									new MouseDownCommand(), 
									new Command.Null<Model>()
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
						collector.execute(new PendingCommandFactory<Model>() {
							@Override
							public Model getReference() {
								return model;
							}

							@Override
							public void createPendingCommand(List<CommandState<Model>> dualCommands) {
								dualCommands.add(new PendingCommandState<Model>(
									new SetPropertyCommand(PROPERTY_COLOR, color),
									new SetPropertyCommand.AfterSetProperty()
								));
							}
						});
					}
				};
			}
		}));
	}

	public void beRemoved(Model reference, ArrayList<Command<Model>> restoreCommands) {
		modelBeRemoved(reference, restoreCommands);
		
		// Assumed location is centered around self
		// What about offsetting locations within canvases? Is this assumption valid then?
		// Perhaps, some sort of location parameter is necessary/could solve this?
		for(Observer observer: new ArrayList<Observer>(observers)) {
			if(observer instanceof Model) {
				Location locationFromReferenceToObservable = ModelComponent.Util.locationBetween(reference, (Model)this);
				Location locationFromReferenceToObserver = ModelComponent.Util.locationBetween(reference, (Model)observer);
				
				restoreCommands.add(new AddObserverCommand(locationFromReferenceToObservable, locationFromReferenceToObserver));
			}
			
			this.removeObserver(observer);
		}
		for(Observer observee: new ArrayList<Observer>(observees)) {
			if(observee instanceof Model) {
				((Model)observee).removeObserver(this);
				
				Location locationFromReferenceToObservable = ModelComponent.Util.locationBetween(reference, (Model)observee);
				Location locationFromReferenceToObserver = ModelComponent.Util.locationBetween(reference, (Model)this);
				
				restoreCommands.add(new AddObserverCommand(locationFromReferenceToObservable, locationFromReferenceToObserver));
			}
		}
	}
	
	protected void modelBeRemoved(Model reference, ArrayList<Command<Model>> restoreCommands) {
		
	}

	public static void appendGeneralDroppedTransactions(final ModelComponent livePanel,
			final ModelComponent dropped, final ModelComponent target, final Rectangle droppedBounds, CompositeMenuBuilder transactions) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addMenuBuilder("Clone Isolated", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;
					
					collector.execute(new PendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return target.getModelBehind();
						}

						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							Location fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							Location fromCCAToDropped = new CompositeLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromCCAToDropped;
							ModelFactory factory = new CloneIsolatedFactory(droppedLocation);
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(creationBounds, factory),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
						}
					});
				}
			});
			
			transactions.addMenuBuilder("Clone Deep", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;
					
					collector.execute(new PendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return target.getModelBehind();
						}

						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							Location fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							Location fromCCAToDropped = new CompositeLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromCCAToDropped;
							ModelFactory factory = new CloneDeepFactory(droppedLocation);
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(creationBounds, factory),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
						}
					});
				}
			});
			
			transactions.addMenuBuilder("New Instance", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;
					
					collector.execute(new PendingCommandFactory<Model>() {
						@Override
						public Model getReference() {
							return target.getModelBehind();
						}

						@Override
						public void createPendingCommand(List<CommandState<Model>> commandStates) {
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							Location fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							Location fromCCAToDropped = new CompositeLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromCCAToDropped;
							ModelFactory factory = new NewInstanceFactory(droppedLocation);
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(creationBounds, factory),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
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
		
		clone.undoStack2.addAll(this.undoStack2);
		clone.redoStack2.addAll(this.redoStack2);
		
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

	public static void executeRemoveObserver(Collector<Model> collector, final ModelComponent observable, final ModelComponent observer) {
		collector.execute(new PendingCommandFactory<Model>() {
			ModelComponent referenceMC;
			
			@Override
			public Model getReference() {
				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
				return referenceMC.getModelBehind();
			}
			
			@Override
			public void createPendingCommand(List<CommandState<Model>> commandStates) {
				Location observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				Location observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);
				
				commandStates.add(new PendingCommandState<Model>(
					new RemoveObserverCommand(observableLocation, observerLocation),
					new AddObserverCommand(observableLocation, observerLocation)
				));
			}
		});
	}

	public static void executeAddObserver(Collector<Model> collector, final ModelComponent observable, final ModelComponent observer) {
		collector.execute(new PendingCommandFactory<Model>() {
			ModelComponent referenceMC;
			
			@Override
			public Model getReference() {
				referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
				return referenceMC.getModelBehind();
			}
			
			@Override
			public void createPendingCommand(List<CommandState<Model>> commandStates) {
				Location observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				Location observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);
				
				commandStates.add(new PendingCommandState<Model>(
					new AddObserverCommand(observableLocation, observerLocation),
					new RemoveObserverCommand(observableLocation, observerLocation)
				));
			}
		});
	}
	
	public abstract Model cloneBase();
}
