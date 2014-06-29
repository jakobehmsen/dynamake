package dynamake.models;

import dynamake.Factory;
import dynamake.tools.Tool;

public interface ViewManager {
	Factory[] getFactories();
	Tool[] getTools();
}