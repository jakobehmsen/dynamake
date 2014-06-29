package dynamake.models;


public class ModelRootLocator implements ModelLocator {
	@Override
	public ModelLocation locate() {
		return new ModelRootLocation();
	}
}