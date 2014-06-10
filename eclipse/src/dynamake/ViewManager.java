package dynamake;

import javax.swing.JComponent;

public interface ViewManager {
	void setFocus(JComponent component);
	void clearFocus(PropogationContext propCtx, PrevaylerServiceBranch<Model> branch);
	int getState();
	Factory[] getFactories();
	void selectAndActive(ModelComponent view, int x, int y);
	void repaint(JComponent view);
	void refresh(ModelComponent view);
	void wasCreated(ModelComponent view);
	Tool[] getTools();
	void unFocus(PropogationContext propCtx, ModelComponent view, PrevaylerServiceBranch<Model> branch);
	void becameVisible(ModelComponent view);
	void becameInvisible(PropogationContext propCtx, ModelComponent view);
}