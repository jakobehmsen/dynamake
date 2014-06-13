package dynamake;

import java.math.BigInteger;

public class Fraction extends Number {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private BigInteger numerator;
	private BigInteger denominator;

	public Fraction(int value) {
		this(value, 1);
	}

	public Fraction(int numerator, int denominator) {
		this.numerator = BigInteger.valueOf(numerator);
		this.denominator = BigInteger.valueOf(denominator);
	}

	public Fraction(BigInteger numerator, BigInteger denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}

	@Override
	public double doubleValue() {
		return numerator.doubleValue() / denominator.doubleValue();
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
			numerator.multiply(other.denominator).add(denominator.multiply(other.numerator)),
			denominator.multiply(other.denominator)
		);
		f.reduce();
		return f;
	}
	
	public Fraction subtract(Fraction other) {
		Fraction f = new Fraction(
			numerator.multiply(other.denominator).subtract(denominator.multiply(other.numerator)),
			denominator.multiply(other.denominator)
		);
		f.reduce();
		return f;
	}
	
	public Fraction multiply(Fraction other) {
		Fraction f = new Fraction(
			numerator.multiply(other.numerator),
			denominator.multiply(other.denominator)
		);
		f.reduce();
		return f;
	}
	
	public Fraction divide(Fraction other) {
		Fraction f = new Fraction(
			numerator.multiply(other.denominator),
			denominator.multiply(other.numerator)
		);
		f.reduce();
		return f;
	}
	
	private void reduce() {
		BigInteger cd = numerator.gcd(denominator);
		if(!cd.equals(BigInteger.ZERO)) {
			numerator = numerator.divide(cd);
			denominator = denominator.divide(cd);
		}
	}
	
	@Override
	public String toString() {
		return numerator + "/" + denominator;
	}
}
