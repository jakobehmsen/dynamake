package dynamake;

import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public abstract class TransactionBuilder {
	public abstract void appendTo(TransactionView view, ArrayList<JMenuItem> menuItems, String name);
	
	public abstract boolean isEmpty();
	
//	public JMenuItem toMenuItem() {
//		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
//		appendTo(view, menuItems);
//		return menuItems.get(0);
//	}

	public JMenuItem toMenu(TransactionView view, String name) {
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

//	public JPopupMenu toPopupMenu() {
//		JPopupMenu popupMenu = new JPopupMenu();
//		
//		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
//		appendTo(view, menuItems);
//		
//		for(JMenuItem menuItem: menuItems)
//			popupMenu.add(menuItem);
//		
//		return popupMenu;
//	}
	
	public void appendTo(final JPopupMenu popupMenu, String name) {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(new TransactionView() {
			@Override
			public void hide() {
				popupMenu.setVisible(false);
			}
		}, menuItems, name);
		
		for(JMenuItem menuItem: menuItems)
			popupMenu.add(menuItem);
	}
}
