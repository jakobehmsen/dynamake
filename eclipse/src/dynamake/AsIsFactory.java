package dynamake;

import java.awt.Rectangle;
import java.util.Hashtable;

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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		return model;
	}
}
