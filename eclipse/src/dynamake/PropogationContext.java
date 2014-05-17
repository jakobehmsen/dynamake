package dynamake;

public class PropogationContext {
	private PropogationContext parent;
	private Model visitedByModel;
	private int tag;
	
	public PropogationContext() { 
		tag = -1;
	}
	
	public PropogationContext(int tag) {
		this.tag = tag;
	}
	
	public boolean isMarkedVisitedBy(Model model) {
		if(visitedByModel != null && visitedByModel == model)
			return true;
		if(parent != null)
			return parent.isMarkedVisitedBy(model);
		return false;
	}
	
	public boolean isTagged(int tag) {
		if(this.tag == tag)
			return true;
		if(parent != null)
			return parent.isTagged(tag);
		return false;
	}

	public PropogationContext markVisitedBy(Model model) {
		PropogationContext newPropCtx = new PropogationContext();
		newPropCtx.parent = this;
		newPropCtx.visitedByModel = model;
		return newPropCtx;
	}

	public PropogationContext tag(int tag) {
		PropogationContext newPropCtx = new PropogationContext();
		newPropCtx.parent = this;
		newPropCtx.tag = tag;
		return newPropCtx;
	}

	public boolean isOrDerivesFrom(PropogationContext propCtx) {
		if(this == propCtx)
			return true;
		if(parent != null)
			return parent.isOrDerivesFrom(propCtx);
		return false;
	}
}
