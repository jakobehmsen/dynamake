package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CreationModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Factory factory;
	private String[] parameterNames;
	
	public CreationModel(Factory factory, String[] parameterNames) {
		this.factory = factory;
		this.parameterNames = parameterNames;
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
				JLabel argumentView = new JLabel(parameterName);
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
	}
	
	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final PanelModel view = new PanelModel(this, transactionFactory);
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, view);

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
