package dynamake;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;

public class ShapeModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Shape";
	}
	
	private ArrayList<ArrayList<Point>> pointsList;

	public ShapeModelFactory(ArrayList<ArrayList<Point>> pointsList) {
		this.pointsList = pointsList;
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		return new ShapeModel(pointsList);
	}
}
