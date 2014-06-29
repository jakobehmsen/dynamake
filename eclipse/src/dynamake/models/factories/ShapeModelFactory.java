package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.ShapeModel;
import dynamake.transcription.TranscriberBranch;

public class ShapeModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Shape";
	}
	
	private ArrayList<ShapeModel.ShapeInfo> shapes;

	public ShapeModelFactory(ArrayList<ShapeModel.ShapeInfo> shapes) {
		this.shapes = shapes;
	}

	@Override
	public Model create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		return new ShapeModel(shapes);
	}
}
