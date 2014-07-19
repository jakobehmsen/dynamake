package dynamake.models;

import dynamake.models.factories.ModelFactory;
import dynamake.tools.ToolFactory;

public interface ViewManager {
	ToolFactory[] getToolFactories();
}