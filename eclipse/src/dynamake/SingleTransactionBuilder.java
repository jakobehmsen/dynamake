package dynamake;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

public class SingleTransactionBuilder extends TransactionBuilder {
	private Runnable action;

	public SingleTransactionBuilder(Runnable action) {
		this.action = action;
	}
	
	@Override
	public void appendTo(final TransactionView view, ArrayList<JMenuItem> menuItems, String name) {
		JMenuItem menuItem = new JMenuItem();
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				view.execute(action);
//				action.run();
			}
		});
		menuItems.add(menuItem);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
