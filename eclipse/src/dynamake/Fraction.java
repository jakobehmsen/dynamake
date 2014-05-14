package dynamake;

public class Fraction extends Number {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Must be replaced by BigInteger to avoid over- and underflow of integers for which cases has already started to occur
	private int numerator;
	private int denominator;

	public Fraction(int value) {
		this(value, 1);
	}

	public Fraction(int numerator, int denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}

	@Override
	public double doubleValue() {
		return (double)numerator / denominator;
	}

	@Override
	public float floatValue() {
		return (float)doubleValue();
	}

	@Override
	public int intValue() {
		return (int)doubleValue();
	}

	@Override
	public long longValue() {
		return (long)doubleValue();
	}
	
	public Fraction add(Fraction other) {
		Fraction f = new Fraction(
			numerator * other.denominator + denominator * other.numerator,
			denominator * other.denominator
		);
		f.reduce();
		return f;
	}
	
	public Fraction subtract(Fraction other) {
		Fraction f = new Fraction(
			numerator * other.denominator - denominator * other.numerator,
			denominator * other.denominator
		);
		f.reduce();
		return f;
	}
	
	public Fraction multiply(Fraction other) {
		Fraction f = new Fraction(
			numerator * other.numerator,
			denominator * other.denominator
		);
		f.reduce();
		return f;
	}
	
	public Fraction divide(Fraction other) {
		Fraction f = new Fraction(
			numerator * other.denominator,
			denominator * other.numerator
		);
		f.reduce();
		return f;
	}
	
	private static int gcd(int m, int n) {
		int r;
		 
		while(n!=0) {
			r = m % n;
			m = n;
			n = r;
		}
		 
		return m;
	}
	
	private void reduce() {
		int cd = gcd(numerator, denominator);
		if(cd > 0) {
			numerator = numerator / cd;
			denominator = denominator / cd;
		}
	}

	
	@Override
	public String toString() {
		return numerator + "/" + denominator;
	}
}
