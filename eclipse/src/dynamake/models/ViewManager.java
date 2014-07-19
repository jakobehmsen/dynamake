package dynamake.models;

import dynamake.models.factories.ModelFactory;
import dynamake.tools.Tool;
import dynamake.tools.ToolFactory;

public interface ViewManager {
	ModelFactory[] getFactories();
	Tool[] getTools();
	ToolFactory[] getToolFactories();
}