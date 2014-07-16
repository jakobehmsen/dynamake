package dynamake.models.factories;


import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class AsIsFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Model model;

	public AsIsFactory(Model model) {
		this.model = model;
	}

	@Override
	public String getName() {
		return "As is";
	}

	@Override
	public Model create(Model rootModel, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		return model;
	}
}
