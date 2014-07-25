package dynamake.menubuilders;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import dynamake.delegates.Action1;
import dynamake.delegates.Func1;
import dynamake.resources.ResourceManager;

public class ColorMenuBuilder extends MenuBuilder {
	private Color initialColor;
	private Func1<Color, Object> actionCreator;

	public ColorMenuBuilder(Color initialColor, Func1<Color, Object> actionCreator) {
		this.initialColor = initialColor;
		this.actionCreator = actionCreator;
	}
	
	private static JColorChooser getColorChooser(String name) {
		try {
			return ResourceManager.INSTANCE.getResource(name, JColorChooser.class);
		} catch (InterruptedException | ExecutionException e) {
			return null;
		}
	}
	
	@Override
	public void appendTo(final MenuView view, ArrayList<JMenuItem> menuItems, String name) {
		JMenu menuItem = new JMenu();
		JColorChooser colorChooser = getColorChooser(name);
		colorChooser.setSelectionModel(new javax.swing.colorchooser.DefaultColorSelectionModel());
		final AbstractColorChooserPanel colorChooserPanel = colorChooser.getChooserPanels()[0];
		
		if(initialColor == null)
			initialColor = UIManager.getColor("Panel.background");
		colorChooserPanel.getColorSelectionModel().setSelectedColor(initialColor);

		colorChooserPanel.getColorSelectionModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				final ChangeListener self = this;

				view.execute(new Action1<ActionRunner>() {
					@Override
					public void run(ActionRunner runner) {
						colorChooserPanel.getColorSelectionModel().removeChangeListener(self);
						Color color = colorChooserPanel.getColorSelectionModel().getSelectedColor();
						Object action = actionCreator.call(color);
						runner.run(action);
						view.hide();
					}
				});
			}
		});

		menuItem.add(colorChooserPanel);
		menuItems.add(menuItem);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
