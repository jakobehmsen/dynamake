package dynamake;

public interface ViewManager {
	Factory[] getFactories();
	void wasCreated(ModelComponent view);
	Tool[] getTools();
	void unFocus(PropogationContext propCtx, ModelComponent view, PrevaylerServiceBranch<Model> branch);
}