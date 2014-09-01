package dynamake.models;


public class ModelRootLocator<T> implements Locator<T> {
	@Override
	public Location<T> locate() {
		return new ModelRootLocation<T>();
	}
}