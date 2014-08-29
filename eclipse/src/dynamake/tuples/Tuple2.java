package dynamake.tuples;

import java.io.Serializable;

public class Tuple2<T1, T2> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final T1 value1;
	public final T2 value2;
	
	public Tuple2(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
}
