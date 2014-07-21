package dynamake.models.factories;


import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class CanvasModelFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		return new CanvasModel();
	}
}
