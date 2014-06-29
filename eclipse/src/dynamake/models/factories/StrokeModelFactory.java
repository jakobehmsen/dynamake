package dynamake.models.factories;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.StrokeModel;
import dynamake.transcription.TranscriberBranch;

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
	public Model create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		return new StrokeModel(creationBounds.getSize(), offset, points);
	}
}
