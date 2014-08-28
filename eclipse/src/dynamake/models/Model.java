package dynamake.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import javax.swing.JComponent;

import dynamake.commands.AddObserverCommand;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateWithOutput;
import dynamake.commands.ExecutionScope;
import dynamake.commands.MappableForwardable;
import dynamake.commands.PURCommand;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RemoveObserverCommand;
import dynamake.commands.ReversibleCommand;
import dynamake.commands.SetPropertyCommand;
import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.menubuilders.ColorMenuBuilder;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.factories.CloneFactory;
import dynamake.models.factories.ModelFactory;
import dynamake.models.factories.DeriveFactory;
import dynamake.models.transcription.NewChangeTransactionHandler;
import dynamake.models.transcription.PostOnlyTransactionHandler;
import dynamake.numbers.Fraction;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.LoadScopeTransactionHandlerFactory;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.Execution;
import dynamake.transcription.Trigger;

/**
 * Instances of implementors are supposed to represent alive-like sensitive entities, each with its own local history.
 */
public abstract class Model implements Serializable, Observer {
	public static class UndoRedoPart implements Serializable, CommandStateWithOutput<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Execution<Model> origin;
		public final CommandStateWithOutput<Model> revertible;
		
		public UndoRedoPart(Execution<Model> origin, CommandStateWithOutput<Model> revertible) {
			this.origin = origin;
			this.revertible = revertible;
		}

		@Override
		public CommandState<Model> executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
			CommandState<Model> reverted = revertible.executeOn(propCtx, prevalentSystem, collector, location, scope);
			return new UndoRedoPart(origin, (CommandStateWithOutput<Model>)reverted);
		}
		
		@Override
		public CommandState<Model> mapToReferenceLocation(Model sourceReference, Model targetReference) {
			return new UndoRedoPart(
				origin.mapToReferenceLocation(sourceReference, targetReference), 
				(CommandStateWithOutput<Model>)revertible.mapToReferenceLocation(sourceReference, targetReference)
			);
		}
		
		@Override
		public CommandState<Model> offset(Location offset) {
			return new UndoRedoPart(origin.offset(offset), (CommandStateWithOutput<Model>)revertible.offset(offset));
		}
		
		public CommandState<Model> forForwarding() {
			return new UndoRedoPart(origin.forForwarding(), (CommandStateWithOutput<Model>)revertible.forForwarding());
		}
		
		public CommandState<Model> forUpwarding() {
			return new UndoRedoPart(origin.forUpwarding(), (CommandStateWithOutput<Model>)revertible.forUpwarding());
		}
		
		@Override
		public void appendPendings(List<CommandState<Model>> pendingCommands) {
			pendingCommands.add(revertible);
		}

		@Override
		public CommandState<Model> forForwarding(Object output) {
			return null;
		}
		
		@Override
		public Object getOutput() {
			return revertible.getOutput();
		}
	}
	
	public static class HistoryAppendLogChange {
		public final List<ReversibleCommand<Model>> pendingUndoablePairs;
		
		public HistoryAppendLogChange(List<ReversibleCommand<Model>> pendingCommands) {
			this.pendingUndoablePairs = pendingCommands;
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
	
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	private ArrayList<Observer> observees = new ArrayList<Observer>();
	protected Hashtable<String, Object> properties = new Hashtable<String, Object>();
	/* Both undo- and stack are assumed to contain RevertingCommandStateSequence<Model> objects */
	protected Stack<HistoryPart> undoStack = new Stack<HistoryPart>();
	protected Stack<HistoryPart> redoStack = new Stack<HistoryPart>();
	
	private Locator locator;
	private Model parent;
	
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
		ous.writeObject(undoStack);
		ous.writeObject(redoStack);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		observers = (ArrayList<Observer>)ois.readObject();
		observees = (ArrayList<Observer>)ois.readObject();
		properties = (Hashtable<String, Object>)ois.readObject();
		undoStack = (Stack<HistoryPart>)ois.readObject();
		redoStack = (Stack<HistoryPart>)ois.readObject();
	}

	public void appendLog(ArrayList<ReversibleCommand<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println("Log");

		redoStack.clear(); // Should the clearing of the redo stack be moved to commitLog?

		sendChanged(new HistoryAppendLogChange(pendingUndoablePairs), propCtx, propDistance, 0, collector);
	}
	
	public void postLog(ArrayList<ReversibleCommand<Model>> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		System.out.println("Log");
		
		sendChanged(new HistoryAppendLogChange(pendingUndoablePairs), propCtx, propDistance, 0, collector);
	}
	
	public static class HistoryPart implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private ExecutionScope scope;
		private List<PURCommand<Model>> purCommands;

		public HistoryPart(ExecutionScope scope, List<PURCommand<Model>> purCommands) {
			// It seems appropriate to not clone the scope, since history parts are always either on the undo- or redo stack
			// - i.e., they are pushed back and forth and thus the scope is not shared and should be up-to-date
			this.scope = scope; 
			
			this.purCommands = purCommands;
		}
		
		public HistoryPart forUndo() {
			ArrayList<PURCommand<Model>> undoables = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> pur: purCommands) {
				// pur.back is assumed to be insignificant
				PURCommand<Model> undoable = pur.inUndoState();
				undoables.add(undoable);
			}
			
			Collections.reverse(undoables);
			
			return new HistoryPart(scope, undoables);
		}
		
		public HistoryPart forRedo() {
			ArrayList<PURCommand<Model>> redoables = new ArrayList<PURCommand<Model>>();
			for(PURCommand<Model> pur: purCommands) {
				// pur.back is assumed to be insignificant
				PURCommand<Model> redoable = pur.inRedoState();
				redoables.add(redoable);
			}
			
			Collections.reverse(redoables);
			
			return new HistoryPart(scope, redoables);
		}
		
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
			collector.execute(purCommands);
		}

		public ExecutionScope getScope() {
			return scope;
		}
	}
	
	public void commitLog(ExecutionScope scope, List<PURCommand<Model>> logPart) {
//		ArrayList<PURCommand<Model>> undoables = new ArrayList<PURCommand<Model>>();
//		for(ReversibleCommand<Model> pur: logPart) {
//			// pur.back is assumed to be insignificant
//			PURCommand<Model> undoable = ((PURCommand<Model>)pur.forth).inUndoState();
//			undoables.add(undoable);
//		}
//		Collections.reverse(undoables);
		
		HistoryPart undoPart = new HistoryPart(scope, logPart).forUndo();
		undoStack.add(undoPart);
	}
	
	public void unplay(int count, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		for(int i = 0; i < count; i++) {
			HistoryPart toUndo = undoStack.pop();
			
			// Probably, unplay should be invoked repeatedly from another outer command somehow, where the
			// scope is derived in a dynamic way (looking it up via the model reference) rather having to
			// serialize the scope via LoadScopeTransactionHandlerFactory.
			collector.startTransaction(this, new LoadScopeTransactionHandlerFactory<Model>(toUndo.scope));
			
			toUndo.executeOn(propCtx, this, collector, new ModelRootLocation());
			
			collector.commitTransaction();
//			CommandState<Model> redoable = toUndo.executeOn(propCtx, this, collector, new ModelRootLocation(), scope);
			HistoryPart redoable = toUndo.forRedo();
			redoStack.push(redoable);
		}
	}	
	
	public void replay(int count, PropogationContext propCtx, int propDistance, Collector<Model> collector, ExecutionScope scope) {
		for(int i = 0; i < count; i++) {
			HistoryPart toRedo = redoStack.pop();
			
			collector.startTransaction(this, new LoadScopeTransactionHandlerFactory<Model>(toRedo.scope));
			
			toRedo.executeOn(propCtx, this, collector, new ModelRootLocation());
			
			collector.commitTransaction();
//			CommandState<Model> undoable = toRedo.executeOn(propCtx, this, collector, new ModelRootLocation(), scope);
			HistoryPart undoable = toRedo.forUndo();
			undoStack.push(undoable);
		}
	}
	
	public HistoryPart undo(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		// An undo method which starts all undo parts and ensure the sequence
		// A new kind of history handler is probably needed?
		
		// undo stack is assumed to consist only of RevertingCommandStateSequence<Model>.
		// These could probably be replaced by simpler structures; just lists of CommandState objects.
//		RevertingCommandStateSequence<Model> toUndo = (RevertingCommandStateSequence<Model>)undoStack.pop();
//		
//		PendingCommandFactory.Util.executeSequence(collector, Arrays.asList(toUndo.commandStates));
//		
//		return toUndo;
		
		HistoryPart toUndo = undoStack.peek();
		
		toUndo.executeOn(propCtx, this, collector, new ModelRootLocation());
		
		return toUndo;
	}

	public void commitUndo() { //CommandState<Model> redoable) {
//		redoStack.push(redoable);

		HistoryPart undone = undoStack.pop();
		redoStack.push(undone.forRedo());
	}
	
	public HistoryPart redo(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
//		RevertingCommandStateSequence<Model> toRedo = (RevertingCommandStateSequence<Model>)redoStack.pop();
//		PendingCommandFactory.Util.executeSequence(collector, Arrays.asList(toRedo.commandStates));
//		
//		return toRedo;
		
		HistoryPart toRedo = redoStack.peek();
		
		toRedo.executeOn(propCtx, this, collector, new ModelRootLocation());
		
		return toRedo;
	}

	public void commitRedo() {
//		undoStack.push(undoable);
		HistoryPart redone = redoStack.pop();
		undoStack.push(redone.forUndo());
	}

	public List<CommandState<Model>> playThenReverse(List<CommandState<Model>> toPlay, PropogationContext propCtx, int propDistance, Collector<Model> collector, ExecutionScope scope) {
		ArrayList<CommandState<Model>> newCommandStates = new ArrayList<CommandState<Model>>();
		
		for(CommandState<Model> cs: toPlay) {
			CommandState<Model> newCS = cs.executeOn(propCtx, this, collector, new ModelRootLocation(), scope); 
			newCommandStates.add(newCS);
		}
		
		Collections.reverse(newCommandStates);
		
		return newCommandStates;
	}

	public boolean canUndo() {
		return undoStack.size() > 0;
	}

	public boolean canRedo() {
		return redoStack.size() > 0;
	}

	public HistoryPart getUndoScope() {
		return undoStack.peek();
	}

	public HistoryPart getRedoScope() {
		return redoStack.peek();
	}
	
//	public CommandState<Model> getUnplayable() {
//		return RevertingCommandStateSequence.reverse(undoStack);
//	}

	public int getLocalChangeCount() {
		return undoStack.size();
	}

	public List<CommandState<Model>> getLocalChanges() {
		ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
		
		// TODO: METHOD DEACTIVETED! MUST BE REIMPLEMENTED!
//		for(CommandState<Model> undoable: undoStack) {
//			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
//			for(int i = 0; i < undoableAsRevertiable.getCommandStateCount(); i++) {
//				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
//				origins.add(undoPart.origin);
//			}
//		}
		
		return origins;
	}

	public List<CommandState<Model>> getLocalChangesBackwards() {
		ArrayList<CommandState<Model>> backwards = new ArrayList<CommandState<Model>>();

		// TODO: METHOD DEACTIVETED! MUST BE REIMPLEMENTED!
//		for(CommandState<Model> undoable: undoStack) {
//			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
//
//			for(int i = undoableAsRevertiable.getCommandStateCount() - 1; i >= 0; i--) {
//				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
//				backwards.add(undoPart.revertible);
//			}
//		}
		
		Collections.reverse(backwards);
		
		return backwards;
	}
	
	public void setLocator(Locator locator) {
//		if(locator == null)
//			System.out.println("Nulled locator of " + this);
//		System.out.println("Set locator to " + locator + " of " + this);
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
	
	public void setProperty(String name, Object value, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(value != null)
			properties.put(name, value);
		else
			properties.remove(name);
		
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

			collector.startTransaction(this, PostOnlyTransactionHandler.class);
			PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
				new SetPropertyCommand(setProperty.name, setProperty.value),
				new SetPropertyCommand.AfterSetProperty()
			));
			collector.commitTransaction();
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

	public void setView(int view, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		setProperty(Model.PROPERTY_VIEW, view, propCtx, propDistance, collector);
	}
	
	public void sendChanged(Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
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
	
	public Observer getObserverWhere(Func1<Observer, Boolean> filter) {
		for(Observer observer: observers) {
			if(filter.call(observer))
				return observer;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Observer> T getObserverOfType(Class<T> c) {
		for(Observer observer: observers) {
			if(c.isInstance(observer))
				return (T)observer;
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Observer> T getObserveeOfType(Class<T> c) {
		for(Observer observee: observees) {
			if(c.isInstance(observee))
				return (T)observee;
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
						PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
							new SetPropertyCommand(PROPERTY_COLOR, color),
							new SetPropertyCommand.AfterSetProperty()
						));
					}
				};
			}
		}));
	}

	public static void appendGeneralDroppedTransactions(final ModelComponent livePanel,
			final ModelComponent dropped, final ModelComponent target, final Rectangle droppedBounds, CompositeMenuBuilder transactions) {
		if(target.getModelBehind() instanceof CanvasModel) {
			transactions.addMenuBuilder("Clone", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;
					
					collector.execute(new Trigger<Model>() {
						public void run(Collector<Model> collector) {
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							Location fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							Location fromTargetToDropped = new CompositeLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromTargetToDropped;
							
							collector.startTransaction(target.getModelBehind(), NewChangeTransactionHandler.class);
							PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(new CloneFactory(new RectangleF(creationBounds), droppedLocation)),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
							collector.commitTransaction();
						}
					});
				}
			});
			
			transactions.addMenuBuilder("Derive", new Trigger<Model>() {
				@Override
				public void run(Collector<Model> collector) {
					final Rectangle creationBounds = droppedBounds;
					
					collector.execute(new Trigger<Model>() {
						public void run(Collector<Model> collector) {
							ModelComponent cca = ModelComponent.Util.closestCommonAncestor(target, dropped);
							Location fromTargetToCCA = ModelComponent.Util.locationToAncestor(cca, target);
							Location fromTargetToDropped = new CompositeLocation(fromTargetToCCA, ModelComponent.Util.locationFromAncestor(cca, dropped));
							// Probably, the "version" of dropped to be cloned is important
							// Dropped may change and, thus, in a undo/redo scenario on target, the newer version is cloned.
							Location droppedLocation = fromTargetToDropped;
							ModelFactory factory = new DeriveFactory(new RectangleF(creationBounds), droppedLocation);
							
							collector.startTransaction(target.getModelBehind(), NewChangeTransactionHandler.class);
							PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand(factory),
								new CanvasModel.RemoveModelCommand.AfterAdd(),
								new CanvasModel.RestoreModelCommand.AfterRemove()
							));
							collector.commitTransaction();
						}
					});
				}
			});
		}
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
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				ModelComponent referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);
				
				Location observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				Location observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);
				
				collector.startTransaction(referenceMC.getModelBehind(), NewChangeTransactionHandler.class);
				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
					new RemoveObserverCommand(observableLocation, observerLocation),
					new AddObserverCommand(observableLocation, observerLocation)
				));
				collector.commitTransaction();
			}
		});
	}

	public static void executeAddObserver(Collector<Model> collector, final ModelComponent observable, final ModelComponent observer) {
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				ModelComponent referenceMC = ModelComponent.Util.closestCommonAncestor(observable, observer);

				Location observableLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observable);
				Location observerLocation = ModelComponent.Util.locationFromAncestor(referenceMC, observer);

				collector.startTransaction(referenceMC.getModelBehind(), NewChangeTransactionHandler.class);
				PendingCommandFactory.Util.executeSingle(collector, new PendingCommandState<Model>(
					new AddObserverCommand(observableLocation, observerLocation),
					new RemoveObserverCommand(observableLocation, observerLocation)
				));
				collector.commitTransaction();
			}
		});
	}
	
	public abstract Model cloneBase();
	
	private static class History implements MappableForwardable, Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final boolean includeHistory;
		public final Stack<HistoryPart> undoStack;
		public final Stack<HistoryPart> redoStack;
		
		public History(boolean includeHistory, Stack<HistoryPart> undoStack, Stack<HistoryPart> redoStack) {
			this.includeHistory = includeHistory;
			this.undoStack = undoStack;
			this.redoStack = redoStack;
		}

		@Override
		public MappableForwardable mapToReferenceLocation(Model sourceReference, Model targetReference) {
			Stack<HistoryPart> mappedUndoStack = new Stack<HistoryPart>();
			
			// TODO: DO MAPPING OF UNDO STACK!
//			for(CommandState<Model> cs: undoStack)
//				mappedUndoStack.add(cs.mapToReferenceLocation(sourceReference, targetReference));

			Stack<HistoryPart> mappedRedoStack = new Stack<HistoryPart>();

			// TODO: DO MAPPING OF UNDO STACK!
//			for(CommandState<Model> cs: redoStack)
//				mappedRedoStack.add(cs.mapToReferenceLocation(sourceReference, targetReference));
			
			return new History(includeHistory, mappedUndoStack, mappedRedoStack);
		}
		
		@Override
		public MappableForwardable forForwarding() {
			Stack<HistoryPart> forForwardingUndoStack = new Stack<HistoryPart>();

			// TODO: DO FORWARDING OF UNDO STACK!
//			for(CommandState<Model> cs: undoStack)
//				forForwardingUndoStack.add(cs.forForwarding());

			Stack<HistoryPart> forForwardingRedoStack = new Stack<HistoryPart>();

			// TODO: DO FORWARDING OF UNDO STACK!
//			for(CommandState<Model> cs: redoStack)
//				forForwardingRedoStack.add(cs.forForwarding());
			
			return new History(includeHistory, forForwardingUndoStack, forForwardingRedoStack);
		}
	}
	
	public MappableForwardable cloneHistory(boolean includeHistory) {
		return new History(includeHistory, undoStack, redoStack);
	}
	
	public void restoreHistory(Object history, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		if(((History)history).includeHistory) {
			this.undoStack = ((History)history).undoStack;
			this.redoStack = ((History)history).redoStack;
		}
		
		ArrayList<CommandState<Model>> origins = new ArrayList<CommandState<Model>>();
		
//		for(CommandState<Model> undoable: ((History)history).undoStack) {
//			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
//			for(int i = 0; i < undoableAsRevertiable.getCommandStateCount(); i++) {
//				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
//				origins.add(undoPart.origin.pending);
//			}
//		}
		
		// TODO: FIGURE OUT HOW TO EXTRACT REVERSIBLE COMMAND TO APPEND TO ORIGINS! AND THEN EXECUTE THEM!
//		for(HistoryPart undoable: ((History)history).undoStack) {
//			RevertingCommandStateSequence<Model> undoableAsRevertiable = (RevertingCommandStateSequence<Model>)undoable;
//			for(int i = 0; i < undoableAsRevertiable.getCommandStateCount(); i++) {
//				UndoRedoPart undoPart = (UndoRedoPart)undoableAsRevertiable.getCommandState(i);
//				origins.add(undoPart.origin.pending);
//			}
//		}
		
		PendingCommandFactory.Util.executeSequence(collector, origins);
	}
	
	public RestorableModel toRestorable(boolean includeLocalHistory) {
		return RestorableModel.wrap(this, includeLocalHistory);
	}

	public void destroy(PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<Execution<Model>> creation = (List<Execution<Model>>)getProperty(RestorableModel.PROPERTY_CREATION);
		if(creation != null) {
			List<CommandState<Model>> destruction = new ArrayList<CommandState<Model>>();
			for(Execution<Model> creationPart: creation)
				destruction.add(creationPart.undoable);
			Collections.reverse(destruction);
			
			PendingCommandFactory.Util.executeSequence(collector, destruction);
		}
	}
}
