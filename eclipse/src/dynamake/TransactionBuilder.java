package dynamake;

import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public abstract class TransactionBuilder {
	public abstract void appendTo(ArrayList<JMenuItem> menuItems);
	
	public abstract boolean isEmpty();
	
	public JMenuItem toMenuItem() {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(menuItems);
		return menuItems.get(0);
	}
	
	public JMenuItem toMenu() {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(menuItems);
		
		if(menuItems.size() == 1) {
			return menuItems.get(0);
		} else {
			JMenu menu = new JMenu();
			
			for(JMenuItem menuItem: menuItems)
				menu.add(menuItem);
			
			return menu;
		}
	}
	
	public JPopupMenu toPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(menuItems);
		
		for(JMenuItem menuItem: menuItems)
			popupMenu.add(menuItem);
		
		return popupMenu;
	}
	
	public void appendTo(JPopupMenu popupMenu) {
		ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		appendTo(menuItems);
		
		for(JMenuItem menuItem: menuItems)
			popupMenu.add(menuItem);
	}
}
