package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;

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
	public Model create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		return model;
	}
}
