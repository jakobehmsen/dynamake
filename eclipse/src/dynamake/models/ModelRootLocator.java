package dynamake.models;


public class ModelRootLocator implements Locator {
	@Override
	public Location locate() {
		return new ModelRootLocation();
	}
}