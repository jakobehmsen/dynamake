package dynamake;

public interface ModelLocator {
	/**
	 * Locates and returns the location of a model at a certain point in time.
	 * @return The location of an element at a certain point in time.
	 */
	ModelLocation locate();
}
