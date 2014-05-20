package dynamake;

import java.util.Date;

public class SetOutputThenRemoveObserver implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location liveModelLocation;
	private Location outputLocation;
	private Location observableLocation;
	private Location observerLocation;

	public SetOutputThenRemoveObserver(Location liveModelLocation, Location outputLocation, Location observableLocation, Location observerLocation) {
		this.liveModelLocation = liveModelLocation;
		this.outputLocation = outputLocation;
		this.observableLocation = observableLocation;
		this.observerLocation = observerLocation;
	}

	@Override
	public void executeOn(PropogationContext propCtx,
			Model prevalentSystem, Date executionTime) {
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
		if(outputLocation != null) {
			Model output = (Model)outputLocation.getChild(prevalentSystem);
			liveModel.setOutput(output, propCtx, 0);
		} else {
			liveModel.setOutput(null, propCtx, 0);
		}
		
		Model observable = (Model)observableLocation.getChild(prevalentSystem);
		Model observer = (Model)observerLocation.getChild(prevalentSystem);
		
		observable.removeObserver(observer);
	}
}