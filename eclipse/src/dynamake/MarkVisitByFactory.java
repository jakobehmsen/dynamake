package dynamake;

import java.awt.Rectangle;
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
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
		Model model = (Model)arguments.get("By");
		
		return new MarkVisit(model);
	}
}
