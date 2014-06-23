package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;

public class StrokeModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Stroke";
	}
	
	private Point offset;
	private ArrayList<Point> points;

	public StrokeModelFactory(Point offset, ArrayList<Point> points) {
		this.offset = offset;
		this.points = points;
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		return new StrokeModel(creationBounds.getSize(), offset, points);
	}
}