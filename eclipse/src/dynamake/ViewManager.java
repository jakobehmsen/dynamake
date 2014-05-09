package dynamake;

import javax.swing.JComponent;
import javax.swing.JTextPane;

public interface ViewManager {
	void setFocus(JComponent component);
	void clearFocus();
	int getState();
	Factory[] getFactories();
	void selectAndActive(ModelComponent view, int x, int y);
	void repaint(JComponent view);
	void refresh(ModelComponent view);
	void wasCreated(ModelComponent view);
	Tool[] getTools();
	void unFocus(ModelComponent view);
}