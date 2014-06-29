package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.TranscriberBranch;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.PropogationContext;

public class CanvasModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Canvas";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		return new CanvasModel();
	}
}
