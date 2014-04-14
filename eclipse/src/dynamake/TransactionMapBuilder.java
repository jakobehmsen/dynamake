package dynamake;

import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
	public void appendTo(ArrayList<JMenuItem> menuItems) {
		for(final TransactionMapBuilder.TransactionInfo tInfo: transactionInfos) {
			JMenuItem menuItem = tInfo.transaction.toMenu();
			menuItem.setText(tInfo.name);
			menuItems.add(menuItem);
		}
	}

	@Override
	public boolean isEmpty() {
		return transactionInfos.isEmpty();
	}
	
//	public JPopupMenu toPopupMenu() {
//		JPopupMenu popupMenu = new JPopupMenu();
//		
//		for(final TransactionMapBuilder.TransactionInfo tInfo: transactionInfos) {
//			JMenuItem menuItem = tInfo.transaction.toMenuItem();
//			menuItem.setText(tInfo.name);
//			
//			popupMenu.add(menuItem);
//		}
//		
//		return popupMenu;
//	}

//	@Override
//	public JMenuItem toMenuItem() {
//		JMenu containerMenuItem = new JMenu();
//		
//		for(final TransactionMapBuilder.TransactionInfo tInfo: transactionInfos) {
//			JMenuItem menuItem = tInfo.transaction.toMenuItem();
//			menuItem.setText(tInfo.name);
//			
//			containerMenuItem.add(menuItem);
//		}
//		
//		return containerMenuItem;
//	}
}
