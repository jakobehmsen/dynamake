package dynamake.menubuilders;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

public class SingleMenuBuilder extends MenuBuilder {
	private Runnable action;

	public SingleMenuBuilder(Runnable action) {
		this.action = action;
	}
	
	@Override
	public void appendTo(final MenuView view, ArrayList<JMenuItem> menuItems, String name) {
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
