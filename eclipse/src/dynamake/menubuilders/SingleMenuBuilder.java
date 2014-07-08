package dynamake.menubuilders;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import dynamake.delegates.Action1;

public class SingleMenuBuilder extends MenuBuilder {
	private Object action;

	public SingleMenuBuilder(Object action) {
		this.action = action;
	}
	
	@Override
	public void appendTo(final MenuView view, ArrayList<JMenuItem> menuItems, String name) {
		JMenuItem menuItem = new JMenuItem();
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				view.execute(new Action1<ActionRunner>() {
					@Override
					public void run(ActionRunner runner) {
						runner.run(action);
					}
				});
//				view.execute(action);
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
