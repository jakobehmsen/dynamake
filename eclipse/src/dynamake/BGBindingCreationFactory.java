package dynamake;

import java.util.Hashtable;

public class BGBindingCreationFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "BG Binding...";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		return new CreationModel(new BGBindingFactory(), new String[]{"Source", "Target"});
	}
}
