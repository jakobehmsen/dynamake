package dynamake;

import java.util.Hashtable;

public class MarkVisitByFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Mark Visit";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model model = (Model)arguments.get("By");
		
		return new MarkVisit(model);
	}
}
