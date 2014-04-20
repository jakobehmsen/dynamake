package dynamake;

public class PropogationContext {
	private PropogationContext parent;
	private Model visitedByModel;
	
	public boolean isMarkedVisitedBy(Model model) {
		if(visitedByModel != null && visitedByModel == model)
			return true;
		if(parent != null)
			return parent.isMarkedVisitedBy(model);
		return false;
	}

	public PropogationContext markVisitedBy(Model model) {
		PropogationContext newPropCtx = new PropogationContext();
		newPropCtx.parent = this;
		newPropCtx.visitedByModel = model;
		return newPropCtx;
	}
}
