package dynamake.models;

import dynamake.models.factories.Factory;
import dynamake.tools.Tool;

public interface ViewManager {
	Factory[] getFactories();
	Tool[] getTools();
}