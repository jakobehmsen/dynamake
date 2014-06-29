package dynamake;

import java.util.ArrayList;

import javax.swing.JMenuItem;

public class CompositeMenuBuilder extends MenuBuilder {
	private static class MenuInfo {
		public final String name;
		public final MenuBuilder builder;
		
		public MenuInfo(String name, MenuBuilder transaction) {
			this.name = name;
			this.builder = transaction;
		}
	}
	
	private ArrayList<CompositeMenuBuilder.MenuInfo> menuInfos = new ArrayList<CompositeMenuBuilder.MenuInfo>();
	
	public void addMenuBuilder(String name, Runnable transaction) {
		addMenudBuilder(name, new SingleMenuBuilder(transaction));
	}
	
	public void addMenudBuilder(String name, MenuBuilder transaction) {
		menuInfos.add(new MenuInfo(name, transaction));
	}
	
	@Override
	public void appendTo(TransactionView view, ArrayList<JMenuItem> menuItems, String name) {
		for(final CompositeMenuBuilder.MenuInfo tInfo: menuInfos) {
			JMenuItem menuItem = tInfo.builder.toMenu(view, tInfo.name);
			menuItem.setText(tInfo.name);
			menuItems.add(menuItem);
		}
	}

	@Override
	public boolean isEmpty() {
		return menuInfos.isEmpty();
	}
}
