package dynamake.models.factories;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.StrokeModel;
import dynamake.transcription.Collector;

public class StrokeModelFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Point offset;
	private ArrayList<Point> points;
	private Rectangle creationBounds;

	public StrokeModelFactory(Point offset, ArrayList<Point> points, Rectangle creationBounds) {
		this.offset = offset;
		this.points = points;
		this.creationBounds = creationBounds;
	}

	@Override
	public ModelCreation create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		return new ModelCreation.Const(new StrokeModel(creationBounds.getSize(), offset, points));
	}

	@Override
	public ModelFactory forForwarding() {
		return this;
	}
}
