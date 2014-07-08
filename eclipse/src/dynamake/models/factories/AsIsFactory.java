package dynamake.models.factories;

import java.awt.Rectangle;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberCollector;

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
	public Model create(Model rootModel, Rectangle creationBounds, PropogationContext propCtx, int propDistance, TranscriberCollector<Model> collector) {
		return model;
	}
}
