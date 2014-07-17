package dynamake.models;


public class ModelRootLocator implements ModelLocator {
	@Override
	public Location locate() {
		return new ModelRootLocation();
	}
}