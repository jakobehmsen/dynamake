package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class CreationModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Factory factory;
	private String[] parameterNames;
	private Hashtable<String, Object> argumentMap = new Hashtable<String, Object>();
	
	public static class ArgumentChanged {
		public final String parameterName;
		public final Object argument;
		
		public ArgumentChanged(String parameterName, Object argument) {
			this.parameterName = parameterName;
			this.argument = argument;
		}
	}
	
	public static class SetArgumentTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location creationLocation;
		private String parameterName;
		private Location argumentLocation;

		public SetArgumentTransaction(Location creationLocation, String parameterName, Location argumentLocation) {
			this.creationLocation = creationLocation;
			this.parameterName = parameterName;
			this.argumentLocation = argumentLocation;
		}

		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection) {
			CreationModel creation = (CreationModel)creationLocation.getChild(prevalentSystem);
			Model argument = (Model)argumentLocation.getChild(prevalentSystem);
//			// HACK: For now, only meta model are used as arguments
//			argument = argument.getMetaModel();
			creation.setArgument(parameterName, argument, new PropogationContext(), 0, connection);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	public CreationModel(Factory factory, String[] parameterNames) {
		this.factory = factory;
		this.parameterNames = parameterNames;
	}
	
	@Override
	public Model modelCloneIsolated() {
		CreationModel clone = new CreationModel(factory, parameterNames);
		
		clone.argumentMap.putAll(this.argumentMap);
		
		return clone;
	}
	
	public void setArgument(String parameterName, Object argument, PropogationContext propCtx, int propDistance, PrevaylerServiceConnection<Model> connection) {
		int i;
		for(i = 0; i < parameterNames.length; i++) {
			if(parameterNames[i].equals(parameterName))
				break;
		}
		
		if(i >= parameterNames.length)
			return;
		
		argumentMap.put(parameterName, argument);
		sendChanged(new ArgumentChanged(parameterName, argument), propCtx, propDistance, 0, connection);
	}
	
	public boolean argumentIsSet(String parameterName) {
		int i;
		for(i = 0; i < parameterNames.length; i++) {
			if(parameterNames[i].equals(parameterName))
				break;
		}
		
		if(i >= parameterNames.length)
			return false;
		
		return argumentMap.containsKey(parameterName);
	}
	
	public boolean allArgumentsAreSet() {
		return argumentMap.size() == parameterNames.length;
	}
	
	private static class ArgumentView extends JLabel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private PanelModel ownerModel;
		private String parameterName;
		
		public ArgumentView(PanelModel ownerModel, String parameterName) {
			super(parameterName);
			this.ownerModel = ownerModel;
			setOpaque(true);
			this.parameterName = parameterName;
		}

		@Override
		public Model getModelBehind() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return ownerModel.getTransactionFactory();
		}
		
		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds,
				Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void appendContainerTransactions(TransactionMapBuilder transactions,
				ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(final ModelComponent target) {
			return new DualCommandFactory<Model>() {
				public DualCommand<Model> createDualCommand() {
					return new DualCommandPair<Model>(
						new SetArgumentTransaction(ownerModel.getTransactionFactory().getModelLocation(), parameterName, target.getTransactionFactory().getModelLocation()),
						null
					);
				}
				
				@Override
				public void createDualCommands(
						List<DualCommand<Model>> dualCommands) {
					dualCommands.add(createDualCommand());
				}
			};
		}

		@Override
		public void initialize() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
		}
	}
	
	private static class PanelModel extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private CreationModel model;
		private TransactionFactory transactionFactory;
		private Hashtable<String, CreationModel.ArgumentView> parameterNameToArgumentViewMap = new Hashtable<String, CreationModel.ArgumentView>();

		public PanelModel(CreationModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			this.setLayout(new BorderLayout());
			this.setBorder(BorderFactory.createRaisedBevelBorder());
			
			JLabel titleLabel = new JLabel("New " + model.factory.getName());
			titleLabel.setForeground(Color.WHITE);
			titleLabel.setBackground(Color.DARK_GRAY);
			titleLabel.setOpaque(true);
			titleLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.add(titleLabel, BorderLayout.NORTH);
			
			JPanel argumentsPanel = new JPanel();
			argumentsPanel.setLayout(new GridLayout(model.parameterNames.length, 1));
			for(String parameterName: model.parameterNames) {
				ArgumentView argumentView = new ArgumentView(this, parameterName);
				argumentView.setBorder(BorderFactory.createLoweredBevelBorder());
				argumentsPanel.add(argumentView);
				parameterNameToArgumentViewMap.put(parameterName, argumentView);
				
				if(model.argumentIsSet(parameterName)) {
					argumentView.setForeground(Color.WHITE);
					argumentView.setBackground(Color.DARK_GRAY);
				}
			}
			this.add(argumentsPanel, BorderLayout.CENTER);
		}

		@Override
		public Model getModelBehind() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		public void showArgumentAsSet(String parameterName) {
			ArgumentView argumentView = parameterNameToArgumentViewMap.get(parameterName);
			if(argumentView != null) {
				argumentView.setForeground(Color.WHITE);
				argumentView.setBackground(Color.DARK_GRAY);
			}
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
			visitAction.run(this);
		}
	}
	
	private static class InstantiateCreationTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location creationLocation;
		private Location canvasLocation;

		public InstantiateCreationTransaction(Location creationLocation, Location canvasLocation) {
			this.creationLocation = creationLocation;
			this.canvasLocation = canvasLocation;
		}


		@Override
		public void executeOn(PropogationContext propCtx, Model arg0, Date arg1, PrevaylerServiceConnection<Model> connection) {
//			PropogationContext propCtx = new PropogationContext();
			
			CreationModel creation = (CreationModel)creationLocation.getChild(arg0);
			CanvasModel canvas = (CanvasModel)canvasLocation.getChild(arg0);
			
			Fraction x = (Fraction)creation.getProperty("X");
			Fraction y = (Fraction)creation.getProperty("Y");
			Fraction width = (Fraction)creation.getProperty("Width");
			Fraction height = (Fraction)creation.getProperty("Height");
			
			Model model = (Model)creation.factory.create(arg0, new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue()), creation.argumentMap, propCtx, 0, connection);

			model.setProperty("X", x, propCtx, 0, connection);
			model.setProperty("Y", y, propCtx, 0, connection);
			model.setProperty("Width", width, propCtx, 0, connection);
			model.setProperty("Height", height, propCtx, 0, connection);
			
			canvas.removeModel(creation, propCtx, 0, connection);
			canvas.addModel(model, propCtx, 0, connection);
		}

//		@Override
//		public Command<Model> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView,
			final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final PanelModel view = new PanelModel(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		final RemovableListener removableListenerForArgumentChanges = Model.RemovableListener.addObserver(this, new ObserverAdapter() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceConnection<Model> connection) {
				if(change instanceof CreationModel.ArgumentChanged) {
					ArgumentChanged argumentChanged = (ArgumentChanged)change;
					
					view.showArgumentAsSet(argumentChanged.parameterName);
					viewManager.refresh(view);
					
					if(((CreationModel)sender).allArgumentsAreSet()) {
						final ModelComponent parent = ModelComponent.Util.getParent(view);
						if(parent != null && parent.getModelBehind() instanceof CanvasModel) {
							connection.execute(propCtx, new DualCommandFactory<Model>() {
								@Override
								public void createDualCommands(List<DualCommand<Model>> dualCommands) {
									dualCommands.add(new DualCommandPair<Model>(
										new InstantiateCreationTransaction(transactionFactory.getModelLocation(), parent.getTransactionFactory().getModelLocation()), 
										null
									));
								}
							});
							connection.commit(new PropogationContext(LiveModel.TAG_CAUSED_BY_COMMIT));
						}
					}
				}
			}
		});
		
		viewManager.wasCreated(view);

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListenerForBoundChanges.releaseBinding();
				removableListenerForArgumentChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
