package dynamake;

public interface Binding<E> {
	E getBindingTarget();
	void releaseBinding();
}