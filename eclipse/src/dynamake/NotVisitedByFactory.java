package dynamake;

import java.awt.Rectangle;
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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		Model model = (Model)arguments.get("By");
		
		return new NotVisited(model);
	}
}
