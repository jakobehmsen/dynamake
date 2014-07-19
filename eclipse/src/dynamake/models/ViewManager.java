package dynamake.models;

import dynamake.models.factories.ModelFactory;
import dynamake.tools.Tool;

public interface ViewManager {
	ModelFactory[] getFactories();
	Tool[] getTools();
}