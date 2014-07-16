package dynamake.models.factories;


import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

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
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		return new CanvasModel();
	}
}
