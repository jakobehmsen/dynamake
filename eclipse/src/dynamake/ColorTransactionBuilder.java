package dynamake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ColorTransactionBuilder extends TransactionBuilder {
	private static Hashtable<String, JColorChooser> nameToColorChooserMap = new Hashtable<String, JColorChooser>();
//	private static JColorChooser colorChooser;
//	private static AbstractColorChooserPanel colorChooserPanel;
	private Color initialColor;
	private Action1<Color> action;

	public ColorTransactionBuilder(Color initialColor, Action1<Color> action) {
		this.initialColor = initialColor;
		this.action = action;
	}
	
	private static JColorChooser getColorChooser(String name) {
		JColorChooser colorChooser = nameToColorChooserMap.get(name);
		if(colorChooser == null) {
			colorChooser = new JColorChooser();
			nameToColorChooserMap.put(name, colorChooser);
		}
		return colorChooser;
	}
	
	@Override
	public void appendTo(final TransactionView view, ArrayList<JMenuItem> menuItems, String name) {
		JMenu menuItem = new JMenu();
//		final JColorChooser colorChooser = new JColorChooser();
		JColorChooser colorChooser = getColorChooser(name);
		colorChooser.setSelectionModel(new javax.swing.colorchooser.DefaultColorSelectionModel());
		final AbstractColorChooserPanel colorChooserPanel = colorChooser.getChooserPanels()[0];
//		JComponent colorChooserPanelC = (JComponent)colorChooserPanel.getComponent(0);
		if(initialColor != null)
			colorChooserPanel.getColorSelectionModel().setSelectedColor(initialColor);

		colorChooserPanel.getColorSelectionModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				colorChooserPanel.getColorSelectionModel().removeChangeListener(this);
				Color color = colorChooserPanel.getColorSelectionModel().getSelectedColor();
				action.run(color);
				view.hide();
			}
		});

//		menuItem.add(colorChooser);
		menuItem.add(colorChooserPanel);
		
		menuItems.add(menuItem);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
