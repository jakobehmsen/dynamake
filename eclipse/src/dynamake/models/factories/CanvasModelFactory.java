package dynamake.models.factories;

import java.awt.Rectangle;

import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberCollector;

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
	public Model create(Model rootModel, Rectangle creationBounds, PropogationContext propCtx, int propDistance, TranscriberCollector<Model> collector) {
		return new CanvasModel();
	}
}
