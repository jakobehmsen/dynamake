package dynamake;

import javax.swing.JComponent;

public abstract class ComponentModel<T extends JComponent> extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TransactionFactory transactionFactory;
	private JComponent component;
	
	public ComponentModel(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
		this.component = createComponent();
	}
	
	protected abstract JComponent createComponent();
	
	protected JComponent getComponent() {
		return component;
	}
	
	protected TransactionFactory getTransactionFactory() {
		return transactionFactory;
	}
}
