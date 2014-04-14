package dynamake;

import java.awt.event.MouseAdapter;

import javax.swing.JComponent;
import javax.swing.JTextPane;

public interface ViewManager {
	void setFocus(JComponent component);
	void clearFocus();
	int getState();
	Factory[] getFactories();
	void registerView(ModelComponent view);
	void unregisterView(ModelComponent view);
	void selectAndActive(ModelComponent view, int x, int y);
	void repaint(JTextPane view);
}