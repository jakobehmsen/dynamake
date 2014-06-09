package dynamake;

import java.util.Date;

import org.prevayler.Transaction;

public class SetViewTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location modelLocation;
	private int view;

	public SetViewTransaction(Location modelLocation, int view) {
		this.modelLocation = modelLocation;
		this.view = view;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
		Model model = (Model)modelLocation.getChild(prevalentSystem);
		model.setView(view, propCtx, 0, 0, branch);
	}
	
	@Override
	public boolean occurredWithin(Location location) {
		return true;
	}
}
