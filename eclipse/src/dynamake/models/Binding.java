package dynamake.models;

public interface Binding<E> {
	E getBindingTarget();
	void releaseBinding();
}