package dynamake.menubuilders;

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
	
	public void addMenuBuilder(String name, Object action) {
		addMenuBuilder(name, new SingleMenuBuilder(action));
	}
	
	public void addMenuBuilder(String name, MenuBuilder transaction) {
		menuInfos.add(new MenuInfo(name, transaction));
	}
	
	@Override
	public void appendTo(MenuView view, ArrayList<JMenuItem> menuItems, String name) {
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
