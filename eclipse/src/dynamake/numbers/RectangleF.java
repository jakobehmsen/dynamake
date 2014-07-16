package dynamake.numbers;

import java.awt.Rectangle;
import java.io.Serializable;

public class RectangleF implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final Fraction x;
	public final Fraction y;
	public final Fraction width;
	public final Fraction height;
	
	public RectangleF(Fraction x, Fraction y, Fraction width, Fraction height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public RectangleF(Rectangle rect) {
		this(new Fraction(rect.x), new Fraction(rect.y), new Fraction(rect.width), new Fraction(rect.height));
	}
}
