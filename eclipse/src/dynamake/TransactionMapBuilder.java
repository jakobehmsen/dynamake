package dynamake;

import java.util.ArrayList;

import javax.swing.JMenuItem;

public class TransactionMapBuilder extends TransactionBuilder {
	private static class TransactionInfo {
		public final String name;
		public final TransactionBuilder transaction;
		
		public TransactionInfo(String name, TransactionBuilder transaction) {
			this.name = name;
			this.transaction = transaction;
		}
	}
	
	private ArrayList<TransactionMapBuilder.TransactionInfo> transactionInfos = new ArrayList<TransactionMapBuilder.TransactionInfo>();
	
	public void addTransaction(String name, Runnable transaction) {
		addTransaction(name, new SingleTransactionBuilder(transaction));
	}
	
	public void addTransaction(String name, TransactionBuilder transaction) {
		transactionInfos.add(new TransactionInfo(name, transaction));
	}
	
	@Override
	public void appendTo(TransactionView view, ArrayList<JMenuItem> menuItems, String name) {
		for(final TransactionMapBuilder.TransactionInfo tInfo: transactionInfos) {
			JMenuItem menuItem = tInfo.transaction.toMenu(view, tInfo.name);
			menuItem.setText(tInfo.name);
			menuItems.add(menuItem);
		}
	}

	@Override
	public boolean isEmpty() {
		return transactionInfos.isEmpty();
	}
}
