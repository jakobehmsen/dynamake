package dynamake;

import java.util.Hashtable;

public class NotVisitedByFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Not Visited";
	}

	@Override
	public Object create(Model rootModel, Hashtable<String, Object> arguments) {
		Model model = (Model)arguments.get("By");
		
		return new NotVisited(model);
	}
}
