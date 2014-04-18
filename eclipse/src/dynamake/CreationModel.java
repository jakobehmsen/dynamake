package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
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
	
	public static class SetArgumentTransaction implements Transaction<CreationModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String parameterName;
		private Location argumentLocation;

		public SetArgumentTransaction(String parameterName, Location argumentLocation) {
			this.parameterName = parameterName;
			this.argumentLocation = argumentLocation;
		}

		@Override
		public void executeOn(CreationModel prevalentSystem, Date executionTime) {
			Model argument = (Model)argumentLocation.getChild(null);
			prevalentSystem.setArgument(parameterName, argument);
		}
	}
	
	public CreationModel(Factory factory, String[] parameterNames) {
		this.factory = factory;
		this.parameterNames = parameterNames;
	}
	
	public void setArgument(String parameterName, Model argument) {
//		sendChanged(new ArgumentChanged());
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
		public Transaction<? extends Model> getDefaultDropTransaction(
				Point dropPoint) {
			Component target = findComponentAt(dropPoint);
			
			if(target instanceof ArgumentView) {
				final ArgumentView argument = (ArgumentView)target;
				argument.setForeground(Color.WHITE);
				argument.setBackground(Color.DARK_GRAY);
			}

			return null;
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final PanelModel view = new PanelModel(this, transactionFactory);
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removeListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
