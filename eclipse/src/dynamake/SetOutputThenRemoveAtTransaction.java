package dynamake;

import java.util.Date;

public class SetOutputThenRemoveAtTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location liveModelLocation;
	private Location outputLocation;
	private Location canvasLocation;
	private int index;

	public SetOutputThenRemoveAtTransaction(Location liveModelLocation, Location outputLocation, Location canvasLocation, int index) {
		this.liveModelLocation = liveModelLocation;
		this.outputLocation = outputLocation;
		this.canvasLocation = canvasLocation;
		this.index = index;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceConnection<Model> connection) {
		LiveModel liveModel = (LiveModel)liveModelLocation.getChild(prevalentSystem);
		if(outputLocation != null) {
			Model output = (Model)outputLocation.getChild(prevalentSystem);
			liveModel.setOutput(output, propCtx, 0, connection);
		} else {
			liveModel.setOutput(null, propCtx, 0, connection);
		}
		
		CanvasModel canvas = (CanvasModel)canvasLocation.getChild(prevalentSystem);
		canvas.removeModel(index, propCtx, 0, connection);
	}
}
