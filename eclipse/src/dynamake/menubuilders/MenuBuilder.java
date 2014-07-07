package dynamake.menubuilders;

import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import dynamake.delegates.Runner;

public abstract class MenuBuilder {
	public abstract void appendTo(MenuView view, ArrayList<JMenuItem> menuItems, String name);
	
	public abstract boolean isEmpty();

	public JMenuItem toMenu(MenuView view, String name) {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(view, menuItems, name);
		
		if(menuItems.size() == 1) {
			return menuItems.get(0);
		} else {
			JMenu menu = new JMenu();
			
			for(JMenuItem menuItem: menuItems)
				menu.add(menuItem);
			
			return menu;
		}
	}
	
	public void appendTo(final JPopupMenu popupMenu, String name) {
		appendTo(popupMenu, new Runner() {
			@Override
			public void run(Runnable runnable) {
				runnable.run();
			}
		}, name);
	}
	
	public void appendTo(final JPopupMenu popupMenu, final Runner runner, String name) {
		appendTo(popupMenu, new ActionRunner() {
			@Override
			public void run(Object action) {
				((Runnable)action).run();
			}
		}, name);
	}
	
	public void appendTo(final JPopupMenu popupMenu, final ActionRunner runner, String name) {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(new MenuView() {
			@Override
			public void hide() {
				popupMenu.setVisible(false);
			}
			
			@Override
			public void execute(Object action) {
				runner.run(action);
			}
		}, menuItems, name);
		
		for(JMenuItem menuItem: menuItems)
			popupMenu.add(menuItem);
	}
}
