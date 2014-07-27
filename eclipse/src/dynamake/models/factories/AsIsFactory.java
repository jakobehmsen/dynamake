package dynamake.models.factories;


import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AsIsFactory implements ModelFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Model model;

	public AsIsFactory(Model model) {
		this.model = model;
	}

	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) {
		return model;
	}
	
	@Override
	public void setup(Model rootModel, Model modelToSetup, PropogationContext propCtx, int propDistance, Collector<Model> collector, Location location) { }
}
