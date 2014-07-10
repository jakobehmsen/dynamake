package dynamake.delegates;

public class Holder<T> implements Action1<T>, Func0<T> {
	private T value;

	@Override
	public T call() {
		return value;
	}

	@Override
	public void run(T arg0) {
		value = arg0;
	}
}
