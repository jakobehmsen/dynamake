package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class CreationModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Factory factory;
	private String[] parameterNames;
	private Hashtable<String, Model> argumentMap = new Hashtable<String, Model>();
	
	public static class ArgumentChanged {
		public final String parameterName;
		public final Model argument;
		
		public ArgumentChanged(String parameterName, Model argument) {
			this.parameterName = parameterName;
			this.argument = argument;
		}
	}
	
	public static class SetArgumentTransaction implements Transaction<Model> {
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
		public void executeOn(Model prevalentSystem, Date executionTime) {
			CreationModel creation = (CreationModel)creationLocation.getChild(prevalentSystem);
			Model argument = (Model)argumentLocation.getChild(prevalentSystem);
			creation.setArgument(parameterName, argument, new PropogationContext());
		}
	}
	
	public CreationModel(Factory factory, String[] parameterNames) {
		this.factory = factory;
		this.parameterNames = parameterNames;
	}
	
	public void setArgument(String parameterName, Model argument, PropogationContext propCtx) {
		int i;
		for(i = 0; i < parameterNames.length; i++) {
			if(parameterNames[i].equals(parameterName))
				break;
		}
		
		if(i >= parameterNames.length)
			return;
		
		argumentMap.put(parameterName, argument);
		sendChanged(new ArgumentChanged(parameterName, argument), propCtx);
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
	
	private static class ArgumentView extends JLabel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String parameterName;
		
		public ArgumentView(String parameterName) {
			super(parameterName);
			setOpaque(true);
			this.parameterName = parameterName;
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
				ArgumentView argumentView = new ArgumentView(parameterName);
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
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
		
		@Override
		public Transaction<Model> getDefaultDropTransaction(
				ModelComponent dropped, Point dropPoint) {
			Component target = findComponentAt(dropPoint);
			
			if(target instanceof ArgumentView) {
				final ArgumentView argument = (ArgumentView)target;
				
//				argument.setForeground(Color.WHITE);
//				argument.setBackground(Color.DARK_GRAY);
				
				return new SetArgumentTransaction(transactionFactory.getLocation(), argument.parameterName, dropped.getTransactionFactory().getLocation());
			}

			return null;
		}

		public void showArgumentAsSet(String parameterName) {
			ArgumentView argumentView = parameterNameToArgumentViewMap.get(parameterName);
			if(argumentView != null) {
				argumentView.setForeground(Color.WHITE);
				argumentView.setBackground(Color.DARK_GRAY);
			}
		}

		@Override
		public void appendDroppedTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final PanelModel view = new PanelModel(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		final RemovableListener removableListenerForArgumentChanges = Model.RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change, PropogationContext propCtx) {
				if(change instanceof CreationModel.ArgumentChanged) {
					ArgumentChanged argumentChanged = (ArgumentChanged)change;
					
					view.showArgumentAsSet(argumentChanged.parameterName);
					viewManager.refresh(view);
					
					if(((CreationModel)sender).allArgumentsAreSet()) {
						
					}
				}
			}
		});

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
