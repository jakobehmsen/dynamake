package dynamake;

import java.util.Date;

import org.prevayler.Transaction;

public class SetViewTransaction implements Transaction<Model> {

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
	public void executeOn(Model prevalentSystem, Date executionTime) {
		PropogationContext propCtx = new PropogationContext();
		Model model = (Model)modelLocation.getChild(prevalentSystem);
		model.setView(view, propCtx, 0, 0);
	}
}