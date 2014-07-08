package dynamake.models.factories;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.StrokeModel;
import dynamake.transcription.TranscriberCollector;

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
	public Model create(Model rootModel, Rectangle creationBounds, PropogationContext propCtx, int propDistance, TranscriberCollector<Model> collector) {
		return new StrokeModel(creationBounds.getSize(), offset, points);
	}
}
