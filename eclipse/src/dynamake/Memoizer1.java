package dynamake;

import java.util.Hashtable;

public class Memoizer1<A0, R> implements Func1<A0, R> {
	private Hashtable<A0, R> valueCache = new Hashtable<A0, R>();
	private Func1<A0, R> valueGetter;

	public Memoizer1(Func1<A0, R> valueGetter) {
		this.valueGetter = valueGetter;
	}

	@Override
	public R call(A0 arg0) {
		R value = valueCache.get(arg0);
		
		if(value == null) {
			value = valueGetter.call(arg0);
			valueCache.put(arg0, value);
		}
		
		return value;
	}
	
	public R get(A0 arg0) {
		return valueCache.get(arg0);
	}
	
	public void clear(A0 arg0) {
		valueCache.remove(arg0);
	}
}
