package dynamake;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

public class CanvasModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Model> models;
	
	public CanvasModel() {
		models = new ArrayList<Model>();
	}
	
	public CanvasModel(ArrayList<Model> models) {
		this.models = models;
	}
	
	public static class AddedModelChange {
		public final int index;
		public final Model model;
		
		public AddedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	@Override
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		for(Model model: models) {
			model.scale(hChange, vChange, propCtx, propDistance, connection, branch);
		}
	}
	
	@Override
	public Model modelCloneIsolated() {
		ArrayList<Model> clonedModels = new ArrayList<Model>();
		
		for(Model model: models) {
			Model clone = model.cloneIsolated();
			clonedModels.add(clone);
		}
		
		return new CanvasModel(clonedModels);
	}
	
	@Override
	protected void modelAddContent(HashSet<Model> contained) {
		for(Model model: models) {
			model.addContent(contained);
		}
	}
	
	@Override
	protected void modelBeRemoved() {
		for(Model model: models) {
			model.beRemoved();
		}
	}

	public int getModelCount() {
		return models.size();
	}

	@Override
	protected void cloneAndMap(Hashtable<Model, Model> sourceToCloneMap) {
		CanvasModel clone = new CanvasModel();
		clone.properties = new Hashtable<String, Object>();
		// Assumed that cloning is not necessary for properties
		// I.e., all property values are immutable
		clone.properties.putAll(this.properties);
		sourceToCloneMap.put(this, clone);
		
		for(Model model: models) {
			model.cloneAndMap(sourceToCloneMap);
			Model modelClone = sourceToCloneMap.get(model);
			clone.models.add(modelClone);
		}
	}
	
	public static class RemovedModelChange {
		public final int index;
		public final Model model;
		
		public RemovedModelChange(int index, Model model) {
			this.index = index;
			this.model = model;
		}
	}
	
	public static class MoveModel2Transaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private Location modelLocation;
		private int indexInTarget;

		public MoveModel2Transaction(Location canvasSourceLocation,
				Location canvasTargetLocation, Location modelLocation, int indexInTarget) {
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.modelLocation = modelLocation;
			this.indexInTarget = indexInTarget;
		}

		@Override
		public void executeOn(PropogationContext propCtx,
				Model prevalentSystem, Date executionTime,
				PrevaylerServiceConnection<Model> connection,
				PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(prevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(prevalentSystem);
			Model model = (Model)modelLocation.getChild(prevalentSystem);
			
			PrevaylerServiceBranch<Model> removeBranch = branch.branch();
			PrevaylerServiceBranch<Model> addBranch = branch.branch();

			int indexOfModel = canvasSource.indexOfModel(model);
			canvasSource.removeModel(indexOfModel, propCtx, 0, connection, removeBranch);
			
			canvasTarget.addModel(indexInTarget, model, propCtx, 0, connection, addBranch);
			
			removeBranch.close();
			addBranch.close();
		}
	}
	
	public static class MoveModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location liveModelLocation;
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
		private Location modelLocation;
		private Point point;
		private boolean setMovedAsOutput;
		
		public MoveModelTransaction(Location liveModelLocation, Location canvasSourceLocation, Location canvasTargetLocation, Location modelLocation, Point point, boolean setMovedAsOutput) {
			this.liveModelLocation = liveModelLocation;
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.modelLocation = modelLocation;
			this.point = point;
			this.setMovedAsOutput = setMovedAsOutput;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(rootPrevalentSystem);
			
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(rootPrevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(rootPrevalentSystem);
			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);

			canvasSource.removeModel(model, propCtx, 0, connection, branch);
			model.beginUpdate(propCtx, 0, connection, branch);
			model.setProperty("X", new Fraction(point.x), propCtx, 0, connection, branch);
			model.setProperty("Y", new Fraction(point.y), propCtx, 0, connection, branch);
			model.endUpdate(propCtx, 0, connection, branch);
			canvasTarget.addModel(model, propCtx, 0, connection, branch);
			if(setMovedAsOutput)
				liveModel.setOutput(model, propCtx, 0, connection, branch);
		}
	}
	
	public static class SetOutputMoveModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location liveModelLocation;
//		private Location outputLocation;
		private Location canvasSourceLocation;
		private Location canvasTargetLocation;
//		private Location modelLocation;
		private int indexSource;
		private int indexTarget;
		private Fraction x;
		private Fraction y;
		
		public SetOutputMoveModelTransaction(Location liveModelLocation, /*Location outputLocation, */Location canvasSourceLocation, Location canvasTargetLocation, int indexSource, int indexTarget, Fraction x, Fraction y) {
			this.liveModelLocation = liveModelLocation;
//			this.outputLocation = outputLocation;
			this.canvasSourceLocation = canvasSourceLocation;
			this.canvasTargetLocation = canvasTargetLocation;
			this.indexSource = indexSource;
			this.indexTarget = indexTarget;
			this.x = x;
			this.y = y;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			LiveModel liveModel = (LiveModel)liveModelLocation.getChild(rootPrevalentSystem);
//			if(outputLocation != null) {
//				Model output = (Model)outputLocation.getChild(rootPrevalentSystem);
//				liveModel.setOutput(output, propCtx, 0);
//			} else {
//				liveModel.setOutput(null, propCtx, 0);
//			}
			
			CanvasModel canvasSource = (CanvasModel)canvasSourceLocation.getChild(rootPrevalentSystem);
			CanvasModel canvasTarget = (CanvasModel)canvasTargetLocation.getChild(rootPrevalentSystem);
//			Model model = (Model)modelLocation.getChild(rootPrevalentSystem);
			
			Model model = canvasSource.getModel(indexSource);

			canvasSource.removeModel(indexSource, propCtx, 0, connection, branch);
			model.beginUpdate(propCtx, 0, connection, branch);
			model.setProperty("X", x, propCtx, 0, connection, branch);
			model.setProperty("Y", y, propCtx, 0, connection, branch);
			model.endUpdate(propCtx, 0, connection, branch);
			canvasTarget.addModel(indexTarget, model, propCtx, 0, connection, branch);
		}
	}
	
	/*
	Obsolete: replace by AddModel2Transaction
	*/
	public static class AddModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		Hashtable<String, Object> creationArgs;
		private Factory factory;
		
		public AddModelTransaction(Location canvasLocation, Rectangle creationBounds, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
			this.creationArgs = new Hashtable<String, Object>();
		}
		
		public AddModelTransaction(Location canvasLocation, Rectangle creationBounds, Hashtable<String, Object> creationArgs, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.creationArgs = creationArgs;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
//			PropogationContext propCtx = new PropogationContext();
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, creationArgs, propCtx, 0, connection, branch);

			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, connection, branch);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, connection, branch);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, connection, branch);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, connection, branch);
			
			canvas.addModel(model, new PropogationContext(), 0, connection, branch);
		}
	}
	
	public static class AddModel2Transaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private Rectangle creationBounds;
		Hashtable<String, Object> creationArgs;
		private Factory factory;
		
		public AddModel2Transaction(Location canvasLocation, Rectangle creationBounds, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.factory = factory;
			this.creationArgs = new Hashtable<String, Object>();
		}
		
		public AddModel2Transaction(Location canvasLocation, Rectangle creationBounds, Hashtable<String, Object> creationArgs, Factory factory) {
			this.canvasLocation = canvasLocation;
			this.creationBounds = creationBounds;
			this.creationArgs = creationArgs;
			this.factory = factory;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model rootPrevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
//			PropogationContext propCtx = new PropogationContext();
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(rootPrevalentSystem);
			Model model = (Model)factory.create(rootPrevalentSystem, creationBounds, creationArgs, propCtx, 0, connection, branch);

			PrevaylerServiceBranch<Model> setPropertyBranch = branch.isolatedBranch();
			model.setProperty("X", new Fraction(creationBounds.x), propCtx, 0, connection, setPropertyBranch);
			model.setProperty("Y", new Fraction(creationBounds.y), propCtx, 0, connection, setPropertyBranch);
			model.setProperty("Width", new Fraction(creationBounds.width), propCtx, 0, connection, setPropertyBranch);
			model.setProperty("Height", new Fraction(creationBounds.height), propCtx, 0, connection, setPropertyBranch);
			
			canvas.addModel(model, new PropogationContext(), 0, connection, branch);
		}
	}
	
	public static class RemoveModelTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location canvasLocation;
		private int index;
		
		public RemoveModelTransaction(Location canvasLocation, int index) {
			this.canvasLocation = canvasLocation;
			this.index = index;
		}
		
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(prevalentSystem);
			Model modelToRemove = canvas.getModel(index);
			canvas.removeModel(index, propCtx, 0, connection, branch);
			modelToRemove.beRemoved();
		}

//		@Override
//		public Command<CanvasModel> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	public void addModel(Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		addModel(models.size(), model, propCtx, propDistance, connection, branch);
	}

	public Model getModel(int index) {
		return models.get(index);
	}

	public void addModel(int index, Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		models.add(index, model);
		sendChanged(new AddedModelChange(index, model), propCtx, propDistance, 0, connection, branch);
	}
	
	public void removeModel(Model model, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		int indexOfModel = indexOfModel(model);
		removeModel(indexOfModel, propCtx, propDistance, connection, branch);
	}
	
	public void removeModel(int index, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		Model model = models.get(index);
		models.remove(index);
		sendChanged(new RemovedModelChange(index, model), propCtx, propDistance, 0, connection, branch);
	}
	
	public static void move(CanvasModel canvasSource, CanvasModel canvasTarget, Model model, int indexInTarget, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
		int indexOfModel = canvasSource.indexOfModel(model);
		canvasSource.models.remove(indexOfModel);
		canvasSource.sendChanged(new RemovedModelChange(indexOfModel, model), propCtx, propDistance, 0, connection, null);
		canvasTarget.models.add(indexInTarget, model);
		canvasTarget.sendChanged(new AddedModelChange(indexInTarget, model), propCtx, propDistance, 0, connection, branch);
	}
	
	public int indexOfModel(Model model) {
		return models.indexOf(model);
	}
	
	private static class CanvasPanel extends JLayeredPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CanvasModel model;
		private TransactionFactory transactionFactory;
		private HashSet<Model> shownModels = new HashSet<Model>();
		private Memoizer1<Model, Binding<ModelComponent>> modelToModelComponentMap;
//		private ArrayList<ModelComponent> viewsInSequence = new ArrayList<ModelComponent>();
		
		public CanvasPanel(final ModelComponent rootView, CanvasModel model, final TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			setLayout(null);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setOpaque(true);
			
			modelToModelComponentMap = new Memoizer1<Model, Binding<ModelComponent>>(new Func1<Model, Binding<ModelComponent>>() {
				@Override
				public Binding<ModelComponent> call(Model model) {
					final Binding<ModelComponent> modelView = model.createView(rootView, viewManager, transactionFactory.extend(new IndexLocator(CanvasPanel.this.model, model)));
					
					Rectangle bounds = new Rectangle(
						((Fraction)model.getProperty("X")).intValue(),
						((Fraction)model.getProperty("Y")).intValue(),
						((Fraction)model.getProperty("Width")).intValue(),
						((Fraction)model.getProperty("Height")).intValue()
					);
					
					((JComponent)modelView.getBindingTarget()).setBounds(bounds);
					
					return modelView;
				}
			});
			
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == 3)
						viewManager.selectAndActive(CanvasPanel.this, e.getX(), e.getY());
				}
			});
		}

		@Override
		public Model getModelBehind() {
			return model;
		}
		
		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, final ModelComponent child, final PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			transactions.addTransaction("Remove", new Runnable() {
				@Override
				public void run() {
					PropogationContext propCtx = new PropogationContext();
					connection.execute(propCtx, new DualCommandFactory<Model>() {
						public DualCommand<Model> createDualCommand() {
							int indexOfModel = model.indexOfModel(child.getModelBehind());
							Location canvasLocation = transactionFactory.getModelLocation();
							
							// TODO: Make the backward transaction
							// The removed model should probably be reconstructed
							// The direct structure (clone isolated) (without observers and observees) could probably be used
							// where this direct structure should, afterwards, be decorated with any missing relations to observers and observees
							Command<Model> backward = null;
							
							return new DualCommandPair<Model>(
								new RemoveModelTransaction(canvasLocation, indexOfModel),
								backward
							);
						}
						
						@Override
						public void createDualCommands(
								List<DualCommand<Model>> dualCommands) {
							dualCommands.add(createDualCommand());
						}
					});
				}
			});
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, transactionFactory, transactions, connection, branch);
		}
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, connection);
		}
		
		@Override
		public void appendDropTargetTransactions(final ModelComponent livePanel,
				final ModelComponent dropped, final Rectangle droppedBounds, final Point dropPoint, TransactionMapBuilder transactions, final PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
			if(dropped.getTransactionFactory().getParent() != null && 
				dropped.getTransactionFactory().getParent() != CanvasPanel.this.transactionFactory &&
				!isContainerOf(dropped.getTransactionFactory(), this.transactionFactory) /*Dropee cannot be child of dropped*/) {
				transactions.addTransaction("Move", new Runnable() {
					@Override
					public void run() {
						PropogationContext propCtx = new PropogationContext();
						connection.execute(propCtx, new DualCommandFactory<Model>() {
							public DualCommand<Model> createDualCommand() {
								Location livePanelLocation = livePanel.getTransactionFactory().getModelLocation();
								Location canvasSourceLocation = dropped.getTransactionFactory().getParent().getModelLocation();
								Location canvasTargetLocation = transactionFactory.getModelLocation();
								Location modelLocation = dropped.getTransactionFactory().getModelLocation();
								
								int indexTarget = ((CanvasModel)getModelBehind()).getModelCount();
								CanvasModel sourceCanvas = (CanvasModel)ModelComponent.Util.getParent(dropped).getModelBehind();
								int indexSource = sourceCanvas.indexOfModel(dropped.getModelBehind());
								CanvasModel targetCanvas = model;
								
								Location canvasTargetLocationAfter;
								int indexOfTargetCanvasInSource = sourceCanvas.indexOfModel(targetCanvas);
								if(indexOfTargetCanvasInSource != -1 && indexSource < indexOfTargetCanvasInSource) {
									// If target canvas is contained with the source canvas, then special care needs to be taken as
									// to predicting the location of target canvas after the move has taken place:
									// - If index of target canvas > index of model to be moved, then the predicated index of target canvas should 1 less
									int predictedIndexOfTargetCanvasInSource = indexOfTargetCanvasInSource - 1;
									canvasTargetLocationAfter = transactionFactory.getParent().extendLocation(new CanvasModel.IndexLocation(predictedIndexOfTargetCanvasInSource));
								} else {
									canvasTargetLocationAfter = canvasTargetLocation;
								}
								
								Fraction x = (Fraction)dropped.getModelBehind().getProperty("X");
								Fraction y = (Fraction)dropped.getModelBehind().getProperty("Y");
								
								return new DualCommandPair<Model>(
									new MoveModelTransaction(livePanelLocation, canvasSourceLocation, canvasTargetLocation, modelLocation, droppedBounds.getLocation(), true), 
									new SetOutputMoveModelTransaction(livePanelLocation, canvasTargetLocationAfter, canvasSourceLocation, indexTarget, indexSource, x, y));
							}
							
							@Override
							public void createDualCommands(
									List<DualCommand<Model>> dualCommands) {
								dualCommands.add(createDualCommand());
							}
						});
					}
				});
			}
		}
		
		private boolean isContainerOf(TransactionFactory container, TransactionFactory item) {
			TransactionFactory parent = item.getParent();
			if(parent != null) {
				if(parent == container)
					return true;
				return isContainerOf(container, parent);
			}
			return false;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void initialize() {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			for(Component child: getComponents())
				((ModelComponent)child).visitTree(visitAction);
			
			visitAction.run(this);
		}
	}
	
	public static class IndexLocator implements ModelLocator {
		private CanvasModel canvasModel;
		private Model model;

		public IndexLocator(CanvasModel canvasModel, Model model) {
			this.canvasModel = canvasModel;
			this.model = model;
		}

		@Override
		public ModelLocation locate() {
			int index = canvasModel.indexOfModel(model);
			return new IndexLocation(index);
		}
	}
	
	public static class IndexLocation implements ModelLocation {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int index;
		
		public IndexLocation(int index) {
			this.index = index;
		}

		@Override
		public Object getChild(Object holder) {
			return ((CanvasModel)holder).models.get(index);
		}

		@Override
		public void setChild(Object holder, Object child) {
			
		}

		@Override
		public Location getModelComponentLocation() {
			return new ViewIndexLocation(index);
		}
	}
	
	private static class ViewIndexLocation implements Location {
		private int index;
		
		public ViewIndexLocation(int index) {
			this.index = index;
		}

		@Override
		public Object getChild(Object holder) {
			// Is the model at index visible? If so, then return the corresponding model. If not, then return null.
			Model model = ((CanvasPanel)holder).model.models.get(index);
			return ((CanvasPanel)holder).modelToModelComponentMap.get(model).getBindingTarget();
		}

		@Override
		public void setChild(Object holder, Object child) {
			
		}
	}

	private void addModelComponent(
			final ModelComponent rootView, 
			final CanvasPanel view, final TransactionFactory transactionFactory, 
			final ViewManager viewManager, 
			Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap, final Model model) {
		Integer viewModel2 = (Integer)model.getProperty(Model.PROPERTY_VIEW);
		if(viewModel2 == null)
			viewModel2 = 1;

		if(view.model.conformsToView(viewModel2)) {
			view.shownModels.add(model);
			
			Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);

			view.add((JComponent)modelView.getBindingTarget());
			view.setComponentZOrder((JComponent)modelView.getBindingTarget(), 0);
			viewManager.becameVisible(modelView.getBindingTarget());
		}
		
		Model.RemovableListener removableListener = Model.RemovableListener.addObserver(model, new Observer() {
			@Override
			public void removeObservee(Observer observee) { }
			
			@Override
			public void changed(Model sender, Object change,
					PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						int modelView2 = (int)propertyChanged.value;
						if(view.model.conformsToView(modelView2)) {
							// Should be shown
							if(!view.shownModels.contains(sender)) {
								Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.add(sender);
								view.add((JComponent)modelView.getBindingTarget());
								int zOrder = view.getComponentCount();
								for(int i = 0; i < models.size(); i++) {
									Model m = models.get(i);
									
									if(view.shownModels.contains(m))
										zOrder--;
									
									if(m == sender)
										break;
								}
								
								view.setComponentZOrder((JComponent)modelView.getBindingTarget(), zOrder);
								viewManager.becameVisible(modelView.getBindingTarget());
								viewManager.refresh(view);
							}
						} else {
							// Should be hidden
							if(view.shownModels.contains(sender)) {
								Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
								
								view.shownModels.remove(sender);
								view.remove((JComponent)modelView.getBindingTarget());
								viewManager.unFocus(propCtx, modelView.getBindingTarget());
								viewManager.becameInvisible(propCtx, modelView.getBindingTarget());
								viewManager.refresh(view);
							}
						}
					}
				}
			}
			
			@Override
			public void addObservee(Observer observee) { }
		});
		
		modelToRemovableListenerMap.put(model, removableListener);
	}

	@Override
	public Binding<ModelComponent> createView(final ModelComponent rootView, final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final CanvasPanel view = new CanvasPanel(rootView, this, transactionFactory, viewManager);
		
		final RemovableListener removableListenerForBoundsChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_BACKGROUND);
		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, view, view, viewManager, Model.COMPONENT_COLOR_BACKGROUND);
		Model.wrapForComponentGUIEvents(this, view, view, viewManager);
		
		final HashSet<Model> shownModels = new HashSet<Model>();
		
		final Hashtable<Model, Model.RemovableListener> modelToRemovableListenerMap = new Hashtable<Model, Model.RemovableListener>();
		for(final Model model: models) {
			addModelComponent(
				rootView, view, transactionFactory, viewManager, 
				modelToRemovableListenerMap, model
			);
		}
		
		final Model.RemovableListener removableListener = Model.RemovableListener.addObserver(this, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, final PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceConnection<Model> connection, PrevaylerServiceBranch<Model> branch) {
				if(change instanceof CanvasModel.AddedModelChange) {
					CanvasModel.AddedModelChange addedChange = (CanvasModel.AddedModelChange)change;
					final Model model = addedChange.model;
					
					addModelComponent(
						rootView, view, transactionFactory, viewManager, 
						modelToRemovableListenerMap, model
					);
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							viewManager.refresh(view);
						}
					});
				} else if(change instanceof CanvasModel.RemovedModelChange) {
					// It could be possible to have map mapping from model to model component as follows:
					Model removedModel = ((CanvasModel.RemovedModelChange)change).model;
					
					Binding<ModelComponent> removedMCBinding = view.modelToModelComponentMap.get(removedModel);
					view.modelToModelComponentMap.clear(removedModel);
					final ModelComponent removedMC = removedMCBinding.getBindingTarget();
					
					Model.RemovableListener removableListener = modelToRemovableListenerMap.get(removedModel);
					removableListener.releaseBinding();
					
					viewManager.becameInvisible(propCtx, removedMC);
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							view.remove((JComponent)removedMC);
							view.validate();
							view.repaint();
							viewManager.unFocus(propCtx, removedMC);
							viewManager.repaint(view);
						}
					});
				} else if(change instanceof Model.PropertyChanged && propDistance == 1) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals(Model.PROPERTY_VIEW)) {
						Hashtable<Integer, Model> invisibles = new Hashtable<Integer, Model>();

						for(int i = 0; i < view.model.models.size(); i++) {
							Model m = view.model.models.get(i);
							boolean wasFound = false;
							for(Component mc: view.getComponents()) {
								if(m == ((ModelComponent)mc).getModelBehind()) {
									wasFound = true;
									break;
								}
							}
							if(!wasFound)
								invisibles.put(i, m);
						}
						
						Hashtable<Integer, Model> newVisibles = new Hashtable<Integer, Model>();

						for(Map.Entry<Integer, Model> entry: invisibles.entrySet()) {
							Model invisible = entry.getValue();
							if(invisible.viewConformsTo((int)propertyChanged.value)) {
								newVisibles.put(entry.getKey(), invisible);
							}
						}
						
						ArrayList<Component> newInvisibles = new ArrayList<Component>();
						for(Component mc: view.getComponents()) {
							if(!((ModelComponent)mc).getModelBehind().viewConformsTo((int)propertyChanged.value)) {
								newInvisibles.add(mc);
							}
						}
						
						for(Component newInvisible: newInvisibles) {
							shownModels.remove(((ModelComponent)newInvisible).getModelBehind());
							view.remove(newInvisible);
							viewManager.becameInvisible(propCtx, (ModelComponent)newInvisible);
						}
						
						Object[] visibles = new Object[view.model.models.size()];
						for(Component mc: view.getComponents()) {
							int indexOfVisible = view.model.indexOfModel(((ModelComponent)mc).getModelBehind());
							visibles[indexOfVisible] = mc;
						}
						
						// Add the new visibles at each their respective index at model into visibles
						for(Map.Entry<Integer, Model> entry: newVisibles.entrySet()) {
							visibles[entry.getKey()] = entry.getValue();
						}
						
						for(int i = 0; i < visibles.length; i++) {
							Object visible = visibles[i];
							if(visible != null) {
								if(visible instanceof Model) {
									// Model to add
									Model model = (Model)visible;
									shownModels.add(model);
									Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);

									view.add((JComponent)modelView.getBindingTarget());
									viewManager.becameVisible(modelView.getBindingTarget());
								}
							}
						}
						
						int zOrder = view.getComponentCount();
						for(int i = 0; i < visibles.length; i++) {
							Object visible = visibles[i];
							if(visible != null) {
								zOrder--;
								if(visible instanceof Model) {
									// Model to add
									Model model = (Model)visible;
									Binding<ModelComponent> modelView = view.modelToModelComponentMap.call(model);
									
									view.setComponentZOrder((JComponent)modelView.getBindingTarget(), zOrder);
								} else {
									JComponent component = (JComponent)visibles[i];
									view.setComponentZOrder(component, zOrder);
								}
							}
						}
						
						viewManager.refresh(view);
					}
				}
			}
		});
		
		viewManager.wasCreated(view);

		return new Binding<ModelComponent>() {
			
			@Override
			public void releaseBinding() {
				removableListenerForComponentPropertyChanges.releaseBinding();
				removableListenerForBoundsChanges.releaseBinding();
				removableListener.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
